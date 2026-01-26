/**
 * Party Page - game selector and navigation
 * Uses localStorage key `soloplay.gameId` to track the current game.
 */

class PartyPage {
    constructor() {
        this.storageKey = 'soloplay.gameId';

        this.root = document.querySelector('.party-controls');
        this.gameSelect = document.getElementById('party-game-select');

        this.init();
    }

    async init() {
        await this.loadGames();

        const initialGameId = this.root?.dataset?.gameId || null;
        const savedGameId = localStorage.getItem(this.storageKey);

        // If we have a game in the URL, use that
        if (initialGameId) {
            this.gameSelect.value = initialGameId;
            localStorage.setItem(this.storageKey, initialGameId);
        } else if (savedGameId) {
            // Redirect to saved game
            this.gameSelect.value = savedGameId;
            window.location.href = `/party/${encodeURIComponent(savedGameId)}`;
        }

        this.bindEvents();
    }

    bindEvents() {
        this.gameSelect?.addEventListener('change', () => {
            const gameId = this.gameSelect.value;
            if (gameId) {
                localStorage.setItem(this.storageKey, gameId);
                window.location.href = `/party/${encodeURIComponent(gameId)}`;
            } else {
                window.location.href = '/party';
            }
        });
    }

    async loadGames() {
        this.gameSelect.innerHTML = '<option value="">Loading...</option>';
        try {
            const resp = await fetch('/api/game', {
                headers: { 'Accept': 'application/json' }
            });
            if (!resp.ok) {
                throw new Error(`HTTP ${resp.status}`);
            }
            const games = await resp.json();
            const options = ['<option value="">Select a game...</option>']
                .concat(
                    (games || []).map(g => {
                        const id = g.gameId || '';
                        const label = g.adventureName ? `${id} — ${g.adventureName}` : id;
                        return `<option value="${this.escapeAttr(id)}">${this.escapeHtml(label)}</option>`;
                    })
                )
                .join('');
            this.gameSelect.innerHTML = options;
        } catch (e) {
            console.error('Failed to load games', e);
            this.gameSelect.innerHTML = '<option value="">Error loading games</option>';
        }
    }

    escapeHtml(value) {
        return String(value)
            .replaceAll('&', '&amp;')
            .replaceAll('<', '&lt;')
            .replaceAll('>', '&gt;')
            .replaceAll('"', '&quot;')
            .replaceAll("'", '&#39;');
    }

    escapeAttr(value) {
        return this.escapeHtml(value).replaceAll('`', '&#96;');
    }
}

if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', () => new PartyPage());
} else {
    new PartyPage();
}
