// Loaded Files Display - Dynamic fetching and rendering of ingested files

// Fetch and display loaded files
async function fetchLoadedFiles() {
    const contentDiv = document.getElementById('loaded-files-content');
    contentDiv.innerHTML = '<p class="loading-text">Loading...</p>';

    try {
        const response = await fetch('/api/lore/files');
        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }
        const files = await response.json();

        if (files.length === 0) {
            contentDiv.innerHTML = '<p class="empty-state">No files loaded yet. Upload some documents to get started.</p>';
        } else {
            let html = '<table class="loaded-files-table">';
            html += '<thead><tr><th>Source File</th><th>Embeddings</th></tr></thead>';
            html += '<tbody>';

            // Sort files by name
            files.sort((a, b) => a.sourceFile.localeCompare(b.sourceFile));

            let totalEmbeddings = 0;
            for (const file of files) {
                html += '<tr>';
                html += `<td class="file-name">${escapeHtml(file.sourceFile)}</td>`;
                html += `<td class="embedding-count">${file.embeddingCount.toLocaleString()}</td>`;
                html += '</tr>';
                totalEmbeddings += file.embeddingCount;
            }

            html += '</tbody>';
            html += '<tfoot><tr><th>Total</th><th>' + totalEmbeddings.toLocaleString() + '</th></tr></tfoot>';
            html += '</table>';
            contentDiv.innerHTML = html;
        }
    } catch (error) {
        console.error('Error fetching loaded files:', error);
        contentDiv.innerHTML = '<p class="error-text">Failed to load files. ' + escapeHtml(error.message) + '</p>';
    }
}

// Utility function to escape HTML
function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

// Initialize when DOM is ready
document.addEventListener('DOMContentLoaded', function() {
    // Refresh button handler
    const refreshBtn = document.getElementById('refresh-btn');
    if (refreshBtn) {
        refreshBtn.addEventListener('click', fetchLoadedFiles);
    }

    // Initial load
    fetchLoadedFiles();
});
