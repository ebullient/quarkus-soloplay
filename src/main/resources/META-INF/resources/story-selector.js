/**
 * Story Thread Selector Module
 * Handles story thread selection and storage in localStorage
 */
class StorySelector {
    constructor() {
        this.currentStoryThread = this.getCurrentStoryThread();
        this.apiEndpoint = '/api/list';

        // Wait for DOM to be ready before initializing
        if (document.readyState === 'loading') {
            document.addEventListener('DOMContentLoaded', () => this.init());
        } else {
            this.init();
        }
    }

    init() {
        // Only proceed if required elements exist
        if (!document.getElementById('story-input')) {
            console.log('Story selector elements not found, skipping initialization');
            return;
        }

        this.updatePageWithNewStoryThread(this.currentStoryThread);
        this.loadStoryThreads();
    }

    getCurrentStoryThread() {
        return localStorage.getItem('currentStoryThread') || 'default';
    }

    setCurrentStoryThread(storyThreadId) {
        localStorage.setItem('currentStoryThread', storyThreadId);
        this.currentStoryThread = storyThreadId;
        this.updatePageWithNewStoryThread(storyThreadId);
    }

    async loadStoryThreads() {
        try {
            const response = await fetch(this.apiEndpoint);
            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }
            const storyThreads = await response.json();
            this.renderStorySelector(storyThreads);
        } catch (error) {
            console.error('Failed to load story threads:', error);
            // Fallback to default story thread
            this.renderStorySelector(['default']);
        }
    }

    renderStorySelector(storyThreads) {
        const inputElement = document.getElementById('story-input');
        const datalist = document.getElementById('story-datalist');
        const setStoryButton = document.getElementById('set-story');
        const refreshButton = document.getElementById('refresh-stories');

        if (!inputElement || !datalist || !setStoryButton || !refreshButton) {
            console.warn('Story selector elements not found');
            return;
        }

        // Add "default" if not in list
        if (!storyThreads.includes('default')) {
            storyThreads.unshift('default');
        }

        // Set current story thread value
        inputElement.value = this.currentStoryThread;

        // Populate datalist options
        datalist.innerHTML = storyThreads.map(storyThread =>
            `<option value="${storyThread}">${storyThread}</option>`
        ).join('');

        // Add event listeners (only once)
        if (!inputElement.hasAttribute('data-listeners-added')) {
            // Handle Enter key to set story thread
            inputElement.addEventListener('keydown', (e) => {
                if (e.key === 'Enter') {
                    this.setStoryFromInput();
                }
            });

            // Set story thread button click
            setStoryButton.addEventListener('click', () => {
                this.setStoryFromInput();
            });

            // Refresh story threads button
            refreshButton.addEventListener('click', () => {
                this.loadStoryThreads();
            });

            inputElement.setAttribute('data-listeners-added', 'true');
        }
    }

    setStoryFromInput() {
        const inputElement = document.getElementById('story-input');
        const storyThreadId = inputElement.value.trim();

        if (storyThreadId) {
            this.setCurrentStoryThread(storyThreadId);
            inputElement.blur(); // Remove focus from input

            // Show feedback that story thread was set
            this.showStorySetFeedback(storyThreadId);
        } else {
            alert('Please enter a story name');
        }
    }

    showStorySetFeedback(storyThreadId) {
        const setStoryButton = document.getElementById('set-story');
        const originalText = setStoryButton.textContent;

        setStoryButton.textContent = '✓ Set!';
        setStoryButton.disabled = true;

        setTimeout(() => {
            setStoryButton.textContent = originalText;
            setStoryButton.disabled = false;
        }, 1500);
    }

    updatePageWithNewStoryThread(storyThreadId) {
        // Update the story thread display in the header
        const storyDisplay = document.getElementById('current-story-display');
        if (storyDisplay) {
            storyDisplay.textContent = `Story: ${storyThreadId}`;
        }

        // Update all story thread-dependent links on the page
        this.updateStoryLinks(storyThreadId);

        // Optionally reload the page to reflect new story thread data
        // Uncomment if you want automatic page refresh on story thread change
        // window.location.search = `?storyThreadId=${encodeURIComponent(storyThreadId)}`;
    }

    updateStoryLinks(storyThreadId) {
        // Update all links that need storyThreadId parameter
        const storyLinks = document.querySelectorAll('a[href*="storyThreadId="]');
        storyLinks.forEach(link => {
            const url = new URL(link.href, window.location.origin);
            url.searchParams.set('storyThreadId', storyThreadId);
            link.href = url.toString();
        });

        // Update links that don't have storyThreadId but should
        const needStoryLinks = document.querySelectorAll('a[href*="/inspect/"]');
        needStoryLinks.forEach(link => {
            const url = new URL(link.href, window.location.origin);
            if (!url.searchParams.has('storyThreadId')) {
                url.searchParams.set('storyThreadId', storyThreadId);
                link.href = url.toString();
            }
        });
    }

    // Utility method to get current story thread for other scripts
    static getCurrentStoryThread() {
        return localStorage.getItem('currentStoryThread') || 'default';
    }

    // Utility method to set story thread from other scripts
    static setCurrentStoryThread(storyThreadId) {
        localStorage.setItem('currentStoryThread', storyThreadId);
        // Dispatch event for other components to listen to
        window.dispatchEvent(new CustomEvent('storyThreadChanged', {
            detail: { storyThreadId }
        }));
    }
}

// Auto-initialize when DOM is loaded
document.addEventListener('DOMContentLoaded', () => {
    // Only initialize if story selector elements exist
    if (document.getElementById('story-input')) {
        new StorySelector();
    }
});

// Export for module usage
if (typeof module !== 'undefined' && module.exports) {
    module.exports = StorySelector;
}
