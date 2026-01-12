/**
 * Character Creator Interface - Conversational character creation
 * Handles communication with /api/story/character-creator endpoint
 */

class CharacterCreatorInterface {
    constructor() {
        this.messagesContainer = document.getElementById('chat-messages');
        this.messageInput = document.getElementById('message-input');
        this.sendButton = document.getElementById('send-button');

        // Story thread info comes from inline script in template
        this.storyThreadId = window.storyThread?.id || window.storyThread?.id;

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
        const loadingMsg = this.addAssistantMessage('Creating your character...');
        loadingMsg.classList.add('loading');

        try {
            const response = await fetch('/api/story/character-creator', {
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

            const data = await response.json();

            // Replace loading message with actual response
            loadingMsg.innerHTML = data.html;
            loadingMsg.classList.remove('loading');

            // Check if character was actually created (verified by server)
            if (data.createdCharacter) {
                this.showCharacterCreatedNotification(data.createdCharacter);
            }

            // Save to history
            this.saveChatHistory();

        } catch (error) {
            console.error('Error sending message:', error);
            loadingMsg.innerHTML = '<p class="error">Error: Could not reach the character creator. Please try again.</p>';
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

    // Show notification when character is created (with server-verified data)
    showCharacterCreatedNotification(character) {
        const notification = document.createElement('div');
        notification.className = 'message system success';
        notification.innerHTML = `
            <p><strong>✅ Character Created: ${character.name}</strong></p>
            ${character.summary ? `<p><em>${character.summary}</em></p>` : ''}
            <p>Your character has been saved to the story. You can now:</p>
            <ul>
                <li><a href="/story/${window.storyThread.id}/play">Start playing</a></li>
                <li><a href="/story/${window.storyThread.id}/character/${encodeURIComponent(character.id)}/edit">Edit character details</a></li>
                <li>Continue chatting to create another character</li>
            </ul>
        `;
        this.messagesContainer.appendChild(notification);
        this.scrollToBottom();
    }

    // Save chat history to localStorage for this character creation session
    saveChatHistory() {
        try {
            const messages = [];
            this.messagesContainer.querySelectorAll('.message:not(.system):not(.success)').forEach(msg => {
                messages.push({
                    role: msg.classList.contains('user') ? 'user' : 'assistant',
                    content: msg.innerHTML
                });
            });

            const key = `chargen-history-${this.storyThreadId}`;
            localStorage.setItem(key, JSON.stringify(messages));
        } catch (e) {
            console.warn('Could not save chat history:', e);
        }
    }

    // Load chat history from localStorage for this character creation session
    loadChatHistory() {
        try {
            const key = `chargen-history-${this.storyThreadId}`;
            const stored = localStorage.getItem(key);

            if (stored) {
                const messages = JSON.parse(stored);

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

    // Clear chat history for this character creation session
    clearHistory() {
        if (confirm('Clear character creation chat history?')) {
            const key = `chargen-history-${this.storyThreadId}`;
            localStorage.removeItem(key);

            // Reset to initial greeting
            const initialGreeting = document.getElementById('initial-greeting');
            this.messagesContainer.innerHTML = '';
            if (initialGreeting) {
                this.messagesContainer.appendChild(initialGreeting.cloneNode(true));
            }
        }
    }
}

// Initialize when DOM is ready
if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', () => {
        window.characterCreatorInterface = new CharacterCreatorInterface();
    });
} else {
    window.characterCreatorInterface = new CharacterCreatorInterface();
}
