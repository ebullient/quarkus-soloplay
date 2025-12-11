/**
 * Campaign Selector Module
 * Handles campaign selection and storage in localStorage
 */
class CampaignSelector {
    constructor() {
        this.currentCampaign = this.getCurrentCampaign();
        this.apiEndpoint = '/campaign/list';
        
        // Wait for DOM to be ready before initializing
        if (document.readyState === 'loading') {
            document.addEventListener('DOMContentLoaded', () => this.init());
        } else {
            this.init();
        }
    }

    init() {
        // Only proceed if required elements exist
        if (!document.getElementById('campaign-input')) {
            console.log('Campaign selector elements not found, skipping initialization');
            return;
        }
        
        this.updatePageWithNewCampaign(this.currentCampaign);
        this.loadCampaigns();
    }

    getCurrentCampaign() {
        return localStorage.getItem('currentCampaign') || 'default';
    }

    setCurrentCampaign(campaignId) {
        localStorage.setItem('currentCampaign', campaignId);
        this.currentCampaign = campaignId;
        this.updatePageWithNewCampaign(campaignId);
    }

    async loadCampaigns() {
        try {
            const response = await fetch(this.apiEndpoint);
            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }
            const campaigns = await response.json();
            this.renderCampaignSelector(campaigns);
        } catch (error) {
            console.error('Failed to load campaigns:', error);
            // Fallback to default campaign
            this.renderCampaignSelector(['default']);
        }
    }

    renderCampaignSelector(campaigns) {
        const inputElement = document.getElementById('campaign-input');
        const datalist = document.getElementById('campaign-datalist');
        const setCampaignButton = document.getElementById('set-campaign');
        const refreshButton = document.getElementById('refresh-campaigns');
        
        if (!inputElement || !datalist || !setCampaignButton || !refreshButton) {
            console.warn('Campaign selector elements not found');
            return;
        }

        // Add "default" if not in list
        if (!campaigns.includes('default')) {
            campaigns.unshift('default');
        }

        // Set current campaign value
        inputElement.value = this.currentCampaign;

        // Populate datalist options
        datalist.innerHTML = campaigns.map(campaign => 
            `<option value="${campaign}">${campaign}</option>`
        ).join('');

        // Add event listeners (only once)
        if (!inputElement.hasAttribute('data-listeners-added')) {
            // Handle Enter key to set campaign
            inputElement.addEventListener('keydown', (e) => {
                if (e.key === 'Enter') {
                    this.setCampaignFromInput();
                }
            });

            // Set campaign button click
            setCampaignButton.addEventListener('click', () => {
                this.setCampaignFromInput();
            });

            // Refresh campaigns button
            refreshButton.addEventListener('click', () => {
                this.loadCampaigns();
            });

            inputElement.setAttribute('data-listeners-added', 'true');
        }
    }

    setCampaignFromInput() {
        const inputElement = document.getElementById('campaign-input');
        const campaignId = inputElement.value.trim();
        
        if (campaignId) {
            this.setCurrentCampaign(campaignId);
            inputElement.blur(); // Remove focus from input
            
            // Show feedback that campaign was set
            this.showCampaignSetFeedback(campaignId);
        } else {
            alert('Please enter a campaign name');
        }
    }

    showCampaignSetFeedback(campaignId) {
        const setCampaignButton = document.getElementById('set-campaign');
        const originalText = setCampaignButton.textContent;
        
        setCampaignButton.textContent = '✓ Set!';
        setCampaignButton.disabled = true;
        
        setTimeout(() => {
            setCampaignButton.textContent = originalText;
            setCampaignButton.disabled = false;
        }, 1500);
    }

    updatePageWithNewCampaign(campaignId) {
        // Update the campaign display in the header
        const campaignDisplay = document.getElementById('current-campaign-display');
        if (campaignDisplay) {
            campaignDisplay.textContent = `Campaign: ${campaignId}`;
        }

        // Update all campaign-dependent links on the page
        this.updateCampaignLinks(campaignId);

        // Optionally reload the page to reflect new campaign data
        // Uncomment if you want automatic page refresh on campaign change
        // window.location.search = `?campaignId=${encodeURIComponent(campaignId)}`;
    }

    updateCampaignLinks(campaignId) {
        // Update all links that need campaignId parameter
        const campaignLinks = document.querySelectorAll('a[href*="campaignId="]');
        campaignLinks.forEach(link => {
            const url = new URL(link.href, window.location.origin);
            url.searchParams.set('campaignId', campaignId);
            link.href = url.toString();
        });

        // Update links that don't have campaignId but should
        const needCampaignLinks = document.querySelectorAll('a[href*="/inspect/"]');
        needCampaignLinks.forEach(link => {
            const url = new URL(link.href, window.location.origin);
            if (!url.searchParams.has('campaignId')) {
                url.searchParams.set('campaignId', campaignId);
                link.href = url.toString();
            }
        });
    }

    // Utility method to get current campaign for other scripts
    static getCurrentCampaign() {
        return localStorage.getItem('currentCampaign') || 'default';
    }

    // Utility method to set campaign from other scripts
    static setCurrentCampaign(campaignId) {
        localStorage.setItem('currentCampaign', campaignId);
        // Dispatch event for other components to listen to
        window.dispatchEvent(new CustomEvent('campaignChanged', { 
            detail: { campaignId } 
        }));
    }
}

// Auto-initialize when DOM is loaded
document.addEventListener('DOMContentLoaded', () => {
    // Only initialize if campaign selector elements exist
    if (document.getElementById('campaign-input')) {
        new CampaignSelector();
    }
});

// Export for module usage
if (typeof module !== 'undefined' && module.exports) {
    module.exports = CampaignSelector;
}