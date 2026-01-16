/**
 * Play Interface - WebSocket chat for solo play
 * Handles streaming communication with `/ws/play/{gameId}`
 */

class PlayInterface {
    constructor() {
        this.messagesContainer = document.getElementById('chat-messages');
        this.messageInput = document.getElementById('message-input');
        this.sendButton = document.getElementById('send-button');

        // Server-assigned session id (populated from the initial "session" message)
        this.sessionId = null;
        // Stable game id used in the WebSocket path
        this.gameId = this.loadOrCreateGameId();

        // WebSocket state
        this.ws = null;
        this.reconnectAttempts = 0;
        this.maxReconnectAttempts = 5;
        this.reconnectDelay = 1000; // Start with 1 second

        // Message tracking
        this.currentAssistantMessage = null;
        this.currentMessageId = null;

        this.setupEventListeners();
        this.setInputEnabled(false);
        this.connect();
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

    // ===== WebSocket Connection =====

    connect() {
        const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
        const wsUrl = `${protocol}//${window.location.host}/ws/play/${encodeURIComponent(this.gameId)}`;

        console.log('Connecting to WebSocket:', wsUrl);
        this.updateConnectionStatus('connecting');

        this.ws = new WebSocket(wsUrl);

        this.ws.onopen = () => {
            console.log('WebSocket connected');
            this.reconnectAttempts = 0;
            this.reconnectDelay = 1000;
            this.updateConnectionStatus('connected');

            // Request conversation history
            this.requestHistory();
        };

        this.ws.onmessage = (event) => {
            try {
                const message = JSON.parse(event.data);
                this.handleServerMessage(message);
            } catch (e) {
                console.error('Error parsing WebSocket message:', e);
            }
        };

        this.ws.onclose = (event) => {
            console.log('WebSocket closed:', event.code, event.reason);
            this.updateConnectionStatus('disconnected');
            this.setInputEnabled(false);

            // Attempt reconnect unless intentionally closed
            if (event.code !== 1000) {
                this.attemptReconnect();
            }
        };

        this.ws.onerror = (error) => {
            console.error('WebSocket error:', error);
            this.updateConnectionStatus('error');
        };
    }

    attemptReconnect() {
        if (this.reconnectAttempts >= this.maxReconnectAttempts) {
            console.error('Max reconnect attempts reached');
            this.addSystemMessage('Connection lost. Please refresh the page.');
            return;
        }

        this.reconnectAttempts++;
        const delay = this.reconnectDelay * Math.pow(2, this.reconnectAttempts - 1);

        console.log(`Reconnecting in ${delay}ms (attempt ${this.reconnectAttempts})`);
        this.updateConnectionStatus('reconnecting');

        setTimeout(() => this.connect(), delay);
    }

    updateConnectionStatus(status) {
        // Update connection indicator if present
        const indicator = document.getElementById('connection-status');
        if (indicator) {
            indicator.className = `connection-status ${status}`;
            indicator.title = status.charAt(0).toUpperCase() + status.slice(1);
        }
    }

    // ===== Message Handling =====

    handleServerMessage(message) {
        const type = message.type;

        switch (type) {
            case 'session':
                console.log('Session established:', message.gameId);
                this.sessionId = message.sessionId;
                if (message.gameId && message.gameId !== this.gameId) {
                    this.gameId = message.gameId;
                    localStorage.setItem('soloplay.gameId', this.gameId);
                }
                this.setInputEnabled(true);
                this.messageInput.focus();
                break;

            case 'history':
                this.handleHistory(message.messages);
                break;

            case 'user_echo':
                this.handleUserEcho(message.senderSessionId, message.text);
                break;

            case 'assistant_start':
                this.handleAssistantStart(message.id);
                break;

            case 'assistant_delta':
                this.handleAssistantDelta(message.id, message.text);
                break;

            case 'assistant_done':
                this.handleAssistantDone(message.id, message.markdown, message.html);
                break;

            case 'error':
                this.handleError(message.id, message.message);
                break;

            default:
                console.warn('Unknown message type:', type);
        }
    }

    loadOrCreateGameId() {
        const existing = localStorage.getItem('soloplay.gameId');
        if (existing) {
            return existing;
        }
        const created = (globalThis.crypto?.randomUUID)
            ? globalThis.crypto.randomUUID()
            : `game-${Date.now()}-${Math.random().toString(16).slice(2)}`;
        localStorage.setItem('soloplay.gameId', created);
        return created;
    }

    requestHistory() {
        if (this.ws && this.ws.readyState === WebSocket.OPEN) {
            this.ws.send(JSON.stringify({
                type: 'history_request',
                limit: 50
            }));
        }
    }

    handleHistory(messages) {
        console.log('Received history:', messages?.length, 'messages');

        // Clear existing messages
        this.messagesContainer.innerHTML = '';

        if (!messages || messages.length === 0) {
            // Fresh start - show appropriate message
            this.showFreshStartMessage();
            return;
        }

        // Restore messages from history
        messages.forEach(msg => {
            if (msg.role === 'user') {
                this.addUserMessage(msg.markdown);
            } else {
                // Use HTML if available, otherwise markdown
                this.addAssistantMessage(msg.html || msg.markdown);
            }
        });

        this.scrollToBottom();
    }

    /**
     * Show fresh start message when no history exists.
     */
    showFreshStartMessage() {
        const msgDiv = document.createElement('div');
        msgDiv.className = 'message system';
        msgDiv.innerHTML = '<p>This seems to be a fresh start. Ready to play?</p>';
        this.messagesContainer.appendChild(msgDiv);
    }

    /**
     * Handle user_echo - broadcast of user message from server.
     * This is how all tabs (including the sender) receive user messages.
     * Avoids duplicate display by checking senderSessionId.
     */
    handleUserEcho(senderSessionId, text) {
        if (this.sessionId && senderSessionId === this.sessionId) {
            return;
        }

        // Message from another tab/user - display it
        console.log('User echo from another connection:', text);
        this.addUserMessage(text);

        // Disable input since generation is starting from another tab
        this.setInputEnabled(false);
    }

    handleAssistantStart(messageId) {
        console.log('Assistant starting:', messageId);
        this.currentMessageId = messageId;

        // Create placeholder for streaming content
        const msgDiv = document.createElement('div');
        msgDiv.className = 'message assistant streaming';
        msgDiv.id = `msg-${messageId}`;

        // Use a pre element for streaming text to preserve formatting
        const streamingContent = document.createElement('div');
        streamingContent.className = 'streaming-content';
        msgDiv.appendChild(streamingContent);

        this.messagesContainer.appendChild(msgDiv);
        this.currentAssistantMessage = msgDiv;
        this.scrollToBottom();
    }

    handleAssistantDelta(messageId, text) {
        if (messageId !== this.currentMessageId || !this.currentAssistantMessage) {
            console.warn('Delta for unknown message:', messageId);
            return;
        }

        // Append text to streaming content
        const streamingContent = this.currentAssistantMessage.querySelector('.streaming-content');
        if (streamingContent) {
            streamingContent.textContent += text;
            this.scrollToBottom();
        }
    }

    handleAssistantDone(messageId, markdown, html) {
        console.log('Assistant done:', messageId);

        if (messageId !== this.currentMessageId || !this.currentAssistantMessage) {
            console.warn('Done for unknown message:', messageId);
            return;
        }

        // Replace streaming content with final HTML
        this.currentAssistantMessage.innerHTML = html;
        this.currentAssistantMessage.classList.remove('streaming');

        // Reset state
        this.currentAssistantMessage = null;
        this.currentMessageId = null;

        // Re-enable input
        this.setInputEnabled(true);
        this.messageInput.focus();
        this.scrollToBottom();
    }

    handleError(messageId, errorMessage) {
        console.error('Server error:', messageId, errorMessage);

        if (this.currentAssistantMessage) {
            this.currentAssistantMessage.innerHTML = `<p class="error">Error: ${errorMessage}</p>`;
            this.currentAssistantMessage.classList.remove('streaming');
            this.currentAssistantMessage = null;
            this.currentMessageId = null;
        } else {
            this.addSystemMessage(`Error: ${errorMessage}`);
        }

        this.setInputEnabled(true);
    }

    // ===== UI Methods =====

    autoResize() {
        this.messageInput.style.height = 'auto';
        this.messageInput.style.height = this.messageInput.scrollHeight + 'px';
    }

    sendMessage() {
        const message = this.messageInput.value.trim();
        if (!message) return;

        if (!this.sessionId) {
            this.addSystemMessage('Connecting...');
            return;
        }

        if (!this.ws || this.ws.readyState !== WebSocket.OPEN) {
            this.addSystemMessage('Not connected. Attempting to reconnect...');
            this.connect();
            return;
        }

        // Disable input while processing
        this.setInputEnabled(false);

        // Add user message to chat immediately (optimistic UI)
        this.addUserMessage(message);

        // Clear input
        this.messageInput.value = '';
        this.autoResize();

        // Send to server (will broadcast to all connections including us)
        this.ws.send(JSON.stringify({
            type: 'user_message',
            text: message
        }));
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
}

// Initialize when DOM is ready
if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', () => {
        window.playInterface = new PlayInterface();
    });
} else {
    window.playInterface = new PlayInterface();
}
