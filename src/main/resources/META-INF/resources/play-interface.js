/**
 * Play Interface - Story-aware chat for solo play
 * Handles communication with /api/story/play endpoint
 */

class PlayInterface {
    constructor() {
        this.messagesContainer = document.getElementById('chat-messages');
        this.messageInput = document.getElementById('message-input');
        this.sendButton = document.getElementById('send-button');

        // Story thread info comes from inline script in template
        this.storyThreadId = window.storyThread?.id || window.storyThread?.slug;

        if (!this.storyThreadId) {
            console.error('No story thread ID found');
            this.addSystemMessage('Error: No story thread selected');
            return;
        }

        this.setupEventListeners();
        this.loadChatHistory();
    }

    setupEventListeners() {
        this.sendButton.addEventListener('click', () => this.sendMessage());

        // Auto-resize textarea
        this.messageInput.addEventListener('input', () => this.autoResize());

        // Handle Enter key (Shift+Enter for new line)
        this.messageInput.addEventListener('keydown', (e) => {
            if (e.key === 'Enter' && !e.shiftKey) {
                e.preventDefault();
                this.sendMessage();
            }
        });
    }

    autoResize() {
        this.messageInput.style.height = 'auto';
        this.messageInput.style.height = this.messageInput.scrollHeight + 'px';
    }

    async sendMessage() {
        const message = this.messageInput.value.trim();
        if (!message) return;

        // Disable input while processing
        this.setInputEnabled(false);

        // Add user message to chat
        this.addUserMessage(message);

        // Clear input
        this.messageInput.value = '';
        this.autoResize(); // Reset height after clearing

        // Add loading indicator
        const loadingMsg = this.addAssistantMessage('Plotting with the GM...');
        loadingMsg.classList.add('loading');

        try {
            const response = await fetch('/api/story/play', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({
                    storyThreadId: this.storyThreadId,
                    message: message
                })
            });

            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }

            const html = await response.text();

            // Replace loading message with actual response
            loadingMsg.innerHTML = html;
            loadingMsg.classList.remove('loading');

            // Save to history
            this.saveChatHistory();

        } catch (error) {
            console.error('Error sending message:', error);
            loadingMsg.innerHTML = '<p class="error">Error: Could not reach the GM. Please try again.</p>';
            loadingMsg.classList.remove('loading');
        } finally {
            this.setInputEnabled(true);
            this.messageInput.focus();
        }
    }

    addUserMessage(text) {
        const msgDiv = document.createElement('div');
        msgDiv.className = 'message user';
        msgDiv.textContent = text;
        this.messagesContainer.appendChild(msgDiv);
        this.scrollToBottom();
        return msgDiv;
    }

    addAssistantMessage(html) {
        const msgDiv = document.createElement('div');
        msgDiv.className = 'message assistant';
        msgDiv.innerHTML = html;
        this.messagesContainer.appendChild(msgDiv);
        this.scrollToBottom();
        return msgDiv;
    }

    addSystemMessage(text) {
        const msgDiv = document.createElement('div');
        msgDiv.className = 'message system';
        msgDiv.textContent = text;
        this.messagesContainer.appendChild(msgDiv);
        this.scrollToBottom();
        return msgDiv;
    }

    scrollToBottom() {
        this.messagesContainer.scrollTop = this.messagesContainer.scrollHeight;
    }

    setInputEnabled(enabled) {
        this.messageInput.disabled = !enabled;
        this.sendButton.disabled = !enabled;
    }

    // Save chat history to localStorage for this story thread
    saveChatHistory() {
        try {
            const messages = [];
            this.messagesContainer.querySelectorAll('.message:not(.system)').forEach(msg => {
                messages.push({
                    role: msg.classList.contains('user') ? 'user' : 'assistant',
                    content: msg.innerHTML
                });
            });

            const key = `chat-history-${this.storyThreadId}`;
            localStorage.setItem(key, JSON.stringify(messages));
        } catch (e) {
            console.warn('Could not save chat history:', e);
        }
    }

    // Load chat history from localStorage for this story thread
    loadChatHistory() {
        try {
            const key = `chat-history-${this.storyThreadId}`;
            const stored = localStorage.getItem(key);

            if (stored) {
                const messages = JSON.parse(stored);

                // Clear current messages except system message
                const systemMsg = this.messagesContainer.querySelector('.message.system');
                this.messagesContainer.innerHTML = '';
                if (systemMsg) {
                    this.messagesContainer.appendChild(systemMsg);
                }

                // Restore messages
                messages.forEach(msg => {
                    if (msg.role === 'user') {
                        this.addUserMessage(msg.content);
                    } else {
                        this.addAssistantMessage(msg.content);
                    }
                });
            }
        } catch (e) {
            console.warn('Could not load chat history:', e);
        }
    }

    // Clear chat history for this thread
    clearHistory() {
        if (confirm('Clear chat history for this story thread?')) {
            const key = `chat-history-${this.storyThreadId}`;
            localStorage.removeItem(key);

            // Reset to welcome message
            const systemMsg = this.messagesContainer.querySelector('.message.system');
            this.messagesContainer.innerHTML = '';
            if (systemMsg) {
                this.messagesContainer.appendChild(systemMsg);
            }
        }
    }
}

// Initialize when DOM is ready
if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', () => {
        window.playInterface = new PlayInterface();
    });
} else {
    window.playInterface = new PlayInterface();
}
