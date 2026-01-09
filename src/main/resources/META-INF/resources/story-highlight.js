/**
 * Story Highlight - Highlights last-played story on selection page
 */
(function() {
    const lastPlayed = localStorage.getItem('currentStoryThread');
    if (!lastPlayed) return;

    const card = document.querySelector(`[data-thread-id="${lastPlayed}"]`);
    if (card) {
        card.classList.add('last-played');
        const badge = card.querySelector('.continue-badge');
        if (badge) badge.style.display = 'inline-block';

        // Scroll the card into view if not visible
        card.scrollIntoView({ behavior: 'smooth', block: 'center' });
    }
})();
