/**
 * Game Index - Modal interactions for game list
 * Handles info and delete modals with REST API calls
 */

class GameIndexController {
    constructor() {
        this.infoModal = document.getElementById('info-modal');
        this.deleteModal = document.getElementById('delete-modal');
        this.currentGameId = null;

        this.setupEventListeners();
    }

    setupEventListeners() {
        // Delegate click events for game cards
        document.addEventListener('click', (e) => {
            const button = e.target.closest('button[data-action]');
            if (!button) return;

            const action = button.dataset.action;
            const gameId = button.dataset.gameId;

            switch (action) {
                case 'info':
                    this.openInfoModal(gameId);
                    break;
                case 'delete':
                    this.openDeleteModal(gameId);
                    break;
                case 'close':
                    this.closeModals();
                    break;
            }
        });

        // Close on backdrop click
        document.querySelectorAll('.modal-backdrop').forEach(backdrop => {
            backdrop.addEventListener('click', () => this.closeModals());
        });

        // Close on Escape key
        document.addEventListener('keydown', (e) => {
            if (e.key === 'Escape') {
                this.closeModals();
            }
        });

        // Confirm delete button
        const confirmDelete = document.getElementById('confirm-delete');
        if (confirmDelete) {
            confirmDelete.addEventListener('click', () => this.confirmDelete());
        }
    }

    openInfoModal(gameId) {
        this.currentGameId = gameId;

        // Show modal with loading state
        const loading = document.getElementById('info-loading');
        const details = document.getElementById('info-details');
        loading.hidden = false;
        details.hidden = true;

        this.infoModal.hidden = false;

        // Fetch game details
        fetch(`/api/game/${encodeURIComponent(gameId)}`)
            .then(response => {
                if (!response.ok) {
                    throw new Error('Game not found');
                }
                return response.json();
            })
            .then(game => {
                document.getElementById('info-game-id').textContent = game.gameId || '-';
                document.getElementById('info-adventure').textContent = game.adventureName || 'None';
                document.getElementById('info-phase').textContent = this.formatPhase(game.gamePhase);
                document.getElementById('info-last-played').textContent = this.formatDate(game.lastPlayedAt);

                loading.hidden = true;
                details.hidden = false;
            })
            .catch(error => {
                console.error('Error fetching game details:', error);
                loading.textContent = 'Error loading game details';
            });
    }

    openDeleteModal(gameId) {
        this.currentGameId = gameId;
        document.getElementById('delete-game-name').textContent = gameId;
        this.deleteModal.hidden = false;
    }

    closeModals() {
        this.infoModal.hidden = true;
        this.deleteModal.hidden = true;
        this.currentGameId = null;

        // Reset loading state for next open
        const loading = document.getElementById('info-loading');
        if (loading) {
            loading.textContent = 'Loading...';
            loading.hidden = false;
        }
    }

    confirmDelete() {
        if (!this.currentGameId) return;

        const gameId = this.currentGameId;
        const confirmBtn = document.getElementById('confirm-delete');

        // Disable button during request
        confirmBtn.disabled = true;
        confirmBtn.textContent = 'Deleting...';

        fetch(`/api/game/${encodeURIComponent(gameId)}`, {
            method: 'DELETE'
        })
            .then(response => {
                if (!response.ok && response.status !== 204) {
                    throw new Error('Failed to delete game');
                }

                // Remove the card from DOM
                const card = document.querySelector(`.card[data-game-id="${gameId}"]`);
                if (card) {
                    card.remove();
                }

                // Check if grid is now empty
                const grid = document.querySelector('.grid');
                if (grid && grid.children.length === 0) {
                    grid.outerHTML = '<div class="card"><p>No games yet. Create one to get started!</p></div>';
                }

                this.closeModals();
            })
            .catch(error => {
                console.error('Error deleting game:', error);
                alert('Failed to delete game. Please try again.');
            })
            .finally(() => {
                confirmBtn.disabled = false;
                confirmBtn.textContent = 'Delete';
            });
    }

    formatPhase(phase) {
        if (!phase) return 'Unknown';
        // Convert SCREAMING_SNAKE to Title Case
        return phase.toLowerCase()
            .split('_')
            .map(word => word.charAt(0).toUpperCase() + word.slice(1))
            .join(' ');
    }

    formatDate(timestamp) {
        if (!timestamp) return 'Never';
        const date = new Date(timestamp);
        return date.toLocaleDateString(undefined, {
            year: 'numeric',
            month: 'short',
            day: 'numeric',
            hour: '2-digit',
            minute: '2-digit'
        });
    }
}

// Initialize when DOM is ready
if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', () => {
        window.gameIndexController = new GameIndexController();
    });
} else {
    window.gameIndexController = new GameIndexController();
}
