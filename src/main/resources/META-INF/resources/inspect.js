/**
 * Inspect Page - user-friendly curl for game state
 * Uses localStorage key `soloplay.gameId` to preselect the current game.
 */

class InspectPage {
    constructor() {
        this.storageKey = 'soloplay.gameId';

        this.root = document.querySelector('.inspect');
        this.gameSelect = document.getElementById('inspect-game-select');
        this.useLocalBtn = document.getElementById('inspect-use-local');
        this.clearLocalBtn = document.getElementById('inspect-clear-local');
        this.refreshBtn = document.getElementById('inspect-refresh');
        this.copyBtn = document.getElementById('inspect-copy');
        this.requestEl = document.getElementById('inspect-request');
        this.statusEl = document.getElementById('inspect-status');
        this.outputEl = document.getElementById('inspect-output');

        this.selectedGameId = null;
        this.selectedEndpoint = 'all';

        this.init();
    }

    async init() {
        this.bindEvents();

        await this.loadGames();

        const initialGameId = this.root?.dataset?.gameId || null;
        const savedGameId = localStorage.getItem(this.storageKey);

        const preferred = initialGameId || savedGameId || this.firstGameIdFromSelect();
        if (preferred) {
            this.setCurrentGame(preferred, { persist: !!preferred });
            await this.refresh();
        } else {
            this.setStatus('Pick a game to inspect.', 'idle');
            this.setRequest('—');
            this.renderJson({});
        }
    }

    bindEvents() {
        document.addEventListener('click', (e) => {
            const endpointButton = e.target.closest('button[data-endpoint]');
            if (endpointButton) {
                this.selectedEndpoint = endpointButton.dataset.endpoint;
                this.highlightActiveEndpoint();
                this.refresh();
            }
        });

        this.gameSelect?.addEventListener('change', () => {
            const gameId = this.gameSelect.value || null;
            if (gameId) {
                this.setCurrentGame(gameId, { persist: true });
                this.refresh();
            }
        });

        this.useLocalBtn?.addEventListener('click', async () => {
            const saved = localStorage.getItem(this.storageKey);
            if (saved) {
                this.setCurrentGame(saved, { persist: true });
                await this.refresh();
                return;
            }
            this.setStatus(`No saved gameId in localStorage (${this.storageKey}).`, 'warn');
        });

        this.clearLocalBtn?.addEventListener('click', () => {
            localStorage.removeItem(this.storageKey);
            this.setStatus('Cleared saved gameId.', 'ok');
        });

        this.refreshBtn?.addEventListener('click', () => this.refresh());
        this.copyBtn?.addEventListener('click', () => this.copyOutput());
    }

    async loadGames() {
        this.gameSelect.innerHTML = '<option value="">Loading…</option>';
        try {
            const games = await this.fetchJson('/api/game');
            const options = ['<option value="">Select a game…</option>']
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
            this.setStatus(`Error loading games: ${e.message}`, 'error');
        }
    }

    firstGameIdFromSelect() {
        const opts = Array.from(this.gameSelect?.options || []);
        const first = opts.find(o => o.value);
        return first ? first.value : null;
    }

    setCurrentGame(gameId, { persist }) {
        this.selectedGameId = gameId;
        if (this.gameSelect) {
            this.gameSelect.value = gameId;
        }
        if (persist) {
            localStorage.setItem(this.storageKey, gameId);
        }
    }

    highlightActiveEndpoint() {
        document.querySelectorAll('button[data-endpoint]').forEach(btn => {
            const endpoint = btn.dataset.endpoint;
            const isActive = endpoint === this.selectedEndpoint;
            btn.classList.toggle('btn-primary', isActive);
            btn.classList.toggle('btn-outline', !isActive);
        });
    }

    async refresh() {
        if (!this.selectedGameId) {
            this.setStatus('Select a game first.', 'warn');
            return;
        }

        this.highlightActiveEndpoint();

        try {
            this.setStatus('Loading…', 'loading');

            const { request, payload } = await this.fetchEndpoint(this.selectedEndpoint, this.selectedGameId);
            this.setRequest(request);
            this.renderJson(payload);
            this.setStatus('OK', 'ok');
        } catch (e) {
            console.error('Inspect refresh failed', e);
            this.setStatus(e.message || 'Request failed', 'error');
        }
    }

    async fetchEndpoint(endpoint, gameId) {
        const enc = encodeURIComponent(gameId);

        const url = (suffix) => `/api/game/${enc}${suffix}`;

        switch (endpoint) {
            case 'game': {
                const request = url('');
                return { request, payload: await this.fetchJson(request) };
            }
            case 'party': {
                const request = url('/party');
                return { request, payload: await this.fetchJson(request) };
            }
            case 'actors': {
                const request = url('/actors');
                return { request, payload: await this.fetchJson(request) };
            }
            case 'locations': {
                const request = url('/locations');
                return { request, payload: await this.fetchJson(request) };
            }
            case 'events': {
                const request = url('/events');
                return { request, payload: await this.fetchJson(request) };
            }
            case 'all':
            default: {
                const request = `GET ${url('')} + related`;
                const [game, party, actors, locations, events] = await Promise.all([
                    this.fetchJson(url('')),
                    this.fetchJson(url('/party')),
                    this.fetchJson(url('/actors')),
                    this.fetchJson(url('/locations')),
                    this.fetchJson(url('/events'))
                ]);
                return { request, payload: { game, party, actors, locations, events } };
            }
        }
    }

    async fetchJson(path) {
        const resp = await fetch(path, {
            headers: {
                'Accept': 'application/json'
            }
        });

        if (!resp.ok) {
            const text = await resp.text().catch(() => '');
            const suffix = text ? ` — ${text}` : '';
            throw new Error(`HTTP ${resp.status} ${resp.statusText}${suffix}`);
        }

        return resp.json();
    }

    renderJson(value) {
        this.outputEl.textContent = JSON.stringify(value, null, 2);
    }

    setRequest(text) {
        this.requestEl.textContent = text || '—';
    }

    setStatus(text, kind) {
        this.statusEl.textContent = text || '—';
        this.statusEl.dataset.kind = kind || 'idle';
    }

    async copyOutput() {
        const text = this.outputEl?.textContent || '';
        if (!text) {
            this.setStatus('Nothing to copy.', 'warn');
            return;
        }

        try {
            if (navigator.clipboard?.writeText) {
                await navigator.clipboard.writeText(text);
                this.setStatus('Copied.', 'ok');
                return;
            }
        } catch (e) {
            // fall through to legacy copy
        }

        try {
            const ta = document.createElement('textarea');
            ta.value = text;
            ta.setAttribute('readonly', 'readonly');
            ta.style.position = 'absolute';
            ta.style.left = '-9999px';
            document.body.appendChild(ta);
            ta.select();
            document.execCommand('copy');
            document.body.removeChild(ta);
            this.setStatus('Copied.', 'ok');
        } catch (e) {
            this.setStatus('Copy failed.', 'error');
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
    document.addEventListener('DOMContentLoaded', () => new InspectPage());
} else {
    new InspectPage();
}

