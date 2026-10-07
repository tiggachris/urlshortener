const API_BASE = window.location.port === '5173' ? 'http://localhost:8080' : '';

let currentShortCode = '';
let currentShortUrl = '';

// Tab Switching
window.switchTab = function(tabName) {
  document.querySelectorAll('.tab-btn').forEach(btn => btn.classList.remove('active'));
  document.querySelectorAll('.tab-content').forEach(view => view.classList.add('hidden'));

  if (tabName === 'shorten') {
    const btn = document.getElementById('tab-shorten-btn');
    const view = document.getElementById('view-shorten');
    if (btn) btn.classList.add('active');
    if (view) view.classList.remove('hidden');
  } else if (tabName === 'analytics') {
    const btn = document.getElementById('tab-analytics-btn');
    const view = document.getElementById('view-analytics');
    if (btn) btn.classList.add('active');
    if (view) view.classList.remove('hidden');
  }
};

// URL Shortening Handler
window.handleShorten = async function(event) {
  event.preventDefault();
  const urlInput = document.getElementById('original-url-input');
  const aliasInput = document.getElementById('custom-alias-input');
  const expiresInput = document.getElementById('expires-input');
  const errorAlert = document.getElementById('shorten-error-alert');
  const resultCard = document.getElementById('result-card');
  const submitBtn = document.getElementById('shorten-submit-btn');

  errorAlert.classList.add('hidden');
  errorAlert.textContent = '';
  submitBtn.disabled = true;
  submitBtn.innerHTML = `<span>Shortening...</span>`;

  const originalUrl = urlInput.value.trim();
  const customAlias = aliasInput ? aliasInput.value.trim() || null : null;
  const expiresInDays = (expiresInput && expiresInput.value) ? parseInt(expiresInput.value, 10) : null;

  try {
    const response = await fetch(`${API_BASE}/api/v1/urls/shorten`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        originalUrl,
        customAlias,
        expiresInDays,
      }),
    });

    const data = await response.json();

    if (!response.ok) {
      if (response.status === 429) {
        throw new Error('You have created too many links recently. Please wait a moment and try again.');
      }
      throw new Error(data.message || data.error || (data.errors ? Object.values(data.errors).join(', ') : 'Unable to shorten this URL.'));
    }

    // Success: Populate Result Card with dynamic live origin URL
    currentShortCode = data.shortCode;
    const baseOrigin = window.location.port === '5173' ? 'http://localhost:8080' : window.location.origin;
    currentShortUrl = `${baseOrigin}/${data.shortCode}`;

    const shortUrlEl = document.getElementById('result-short-url');
    if (shortUrlEl) {
      shortUrlEl.href = currentShortUrl;
      shortUrlEl.textContent = currentShortUrl;
    }

    const origUrlEl = document.getElementById('result-original-url');
    if (origUrlEl) {
      origUrlEl.textContent = data.originalUrl;
    }

    const cacheBadge = document.getElementById('cache-badge');
    if (cacheBadge) {
      cacheBadge.textContent = '✨ Your short link is ready!';
    }

    resultCard.classList.remove('hidden');
  } catch (err) {
    errorAlert.textContent = err.message;
    errorAlert.classList.remove('hidden');
    resultCard.classList.add('hidden');
  } finally {
    submitBtn.disabled = false;
    submitBtn.innerHTML = `<span>Shorten</span><span class="arrow-icon">→</span>`;
  }
};

// Copy URL to Clipboard
window.copyResultUrl = function() {
  if (!currentShortUrl) return;
  navigator.clipboard.writeText(currentShortUrl).then(() => {
    const copyText = document.getElementById('copy-btn-text');
    if (copyText) {
      copyText.textContent = '✅ Copied!';
      setTimeout(() => {
        copyText.textContent = '📋 Copy';
      }, 2000);
    }
  });
};

// Inspect Analytics From Shortened Result
window.inspectAnalyticsFromResult = function() {
  if (!currentShortCode) return;
  const input = document.getElementById('analytics-code-input');
  if (input) input.value = currentShortCode;
  window.switchTab('analytics');
  window.fetchAnalyticsData();
};

// Test Live Redirect
window.testLiveRedirect = function() {
  if (!currentShortUrl) return;
  window.open(currentShortUrl, '_blank');
};

// Fetch Analytics
window.fetchAnalyticsData = async function() {
  const codeInput = document.getElementById('analytics-code-input');
  const code = codeInput ? codeInput.value.trim() : '';
  const errorAlert = document.getElementById('analytics-error-alert');
  const dataContainer = document.getElementById('analytics-data-container');
  const loading = document.getElementById('analytics-loading');

  if (!code) {
    if (errorAlert) {
      errorAlert.textContent = 'Please enter a short code or alias to view stats.';
      errorAlert.classList.remove('hidden');
    }
    if (dataContainer) dataContainer.classList.add('hidden');
    return;
  }

  if (errorAlert) errorAlert.classList.add('hidden');
  if (dataContainer) dataContainer.classList.add('hidden');
  if (loading) loading.classList.remove('hidden');

  try {
    const res = await fetch(`${API_BASE}/api/v1/urls/${encodeURIComponent(code)}/analytics`);
    const data = await res.json();

    if (!res.ok) {
      throw new Error(data.message || 'No stats found for link: ' + code);
    }

    // Populate Metrics
    const clicksEl = document.getElementById('metric-total-clicks');
    if (clicksEl) clicksEl.textContent = (data.totalClicks || 0).toLocaleString();

    const origEl = document.getElementById('metric-original-url');
    if (origEl) {
      origEl.textContent = data.originalUrl;
      origEl.title = data.originalUrl;
    }

    // Clean top referrers
    const cleanedReferrers = {};
    Object.entries(data.topReferrers || {}).forEach(([ref, count]) => {
      const cleanName = formatReferrer(ref);
      cleanedReferrers[cleanName] = (cleanedReferrers[cleanName] || 0) + count;
    });
    const topReferrerEntry = Object.entries(cleanedReferrers).sort((a, b) => b[1] - a[1])[0];
    const topRefEl = document.getElementById('metric-top-referrer');
    if (topRefEl) topRefEl.textContent = topReferrerEntry ? topReferrerEntry[0] : 'Direct';

    // Populate Browser Breakdown
    renderBreakdown('browser-breakdown-list', data.clicksByBrowser, data.totalClicks);

    // Populate Device Breakdown
    renderBreakdown('device-breakdown-list', data.clicksByDevice, data.totalClicks);

    // Populate Recent Events Table
    const tbody = document.getElementById('recent-events-tbody');
    if (tbody) {
      tbody.innerHTML = '';
      if (data.recentClicks && data.recentClicks.length > 0) {
        data.recentClicks.forEach(event => {
          const tr = document.createElement('tr');
          const formattedDate = new Date(event.timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' });
          tr.innerHTML = `
            <td>${formattedDate}</td>
            <td>${escapeHtml(event.browser)}</td>
            <td>${escapeHtml(event.deviceType)}</td>
            <td>${escapeHtml(formatReferrer(event.referrer))}</td>
            <td><code>${escapeHtml(formatVisitorIp(event.maskedIp))}</code></td>
          `;
          tbody.appendChild(tr);
        });
      } else {
        tbody.innerHTML = `<tr><td colspan="5" style="text-align: center; color: #64748b; padding: 1.5rem;">No clicks recorded yet. Share your link to see visitor stats!</td></tr>`;
      }
    }

    if (dataContainer) dataContainer.classList.remove('hidden');
  } catch (err) {
    if (errorAlert) {
      errorAlert.textContent = err.message;
      errorAlert.classList.remove('hidden');
    }
  } finally {
    if (loading) loading.classList.add('hidden');
  }
};

function renderBreakdown(containerId, dataMap, total) {
  const container = document.getElementById(containerId);
  if (!container) return;
  container.innerHTML = '';
  const entries = Object.entries(dataMap || {});
  if (entries.length === 0) {
    container.innerHTML = `<div style="color: #64748b; font-size: 0.85rem;">No data recorded yet</div>`;
    return;
  }

  entries.sort((a, b) => b[1] - a[1]).forEach(([name, count]) => {
    const percent = total > 0 ? Math.round((count / total) * 100) : 0;
    const item = document.createElement('div');
    item.className = 'breakdown-item';
    item.innerHTML = `
      <div class="breakdown-meta">
        <span>${escapeHtml(name)}</span>
        <span><strong>${count}</strong> (${percent}%)</span>
      </div>
      <div class="breakdown-bar-bg">
        <div class="breakdown-bar-fill" style="width: ${percent}%;"></div>
      </div>
    `;
    container.appendChild(item);
  });
}

function escapeHtml(str) {
  if (!str) return '';
  return str.replace(/[&<>"']/g, m => ({
    '&': '&amp;',
    '<': '&lt;',
    '>': '&gt;',
    '"': '&quot;',
    "'": '&#39;'
  })[m]);
}

function formatReferrer(ref) {
  if (!ref || ref === 'Direct' || ref.includes('localhost') || ref.includes('127.0.0.1')) {
    return 'Direct';
  }
  try {
    const url = new URL(ref.startsWith('http') ? ref : `https://${ref}`);
    return url.hostname.replace(/^www\./, '');
  } catch {
    return ref.replace(/^https?:\/\//, '').replace(/\/$/, '');
  }
}

function formatVisitorIp(ip) {
  if (!ip || ip.includes('0:0:0') || ip === '::1' || ip.startsWith('127.')) {
    return 'Local Client';
  }
  return ip;
}
