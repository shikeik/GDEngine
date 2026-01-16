/**
 * GDEngine Changelog Renderer (v3.2 - Fix LineBreaks & Style)
 */

console.log("[JS-Probe] changelog.js 已加载");

function renderChangelog(data, currentVersion) {
	console.log(`[JS-Probe] 开始渲染: 数据版本=${data.lastUpdated}`);

	let html = `<div class="changelog-container">`;
	let updateTime = data.lastUpdated || "Unknown";

	html += `<div class="log-meta">Last Updated: ${updateTime} &nbsp;|&nbsp; Local Engine: <span style="color:#000; background:#09D2B8; padding:2px 6px; border-radius:4px; color:white;">${currentVersion}</span></div>`;

	if (!data.groups) {
		return `<div class="empty-log">Error: Invalid Data Structure (Missing groups)</div>`;
	}

	data.groups.forEach(group => {
		let groupId = group.id;

		let status = 'history';
		if (groupId === "In Development") status = 'future';
		else {
			let diff = compareVersions(groupId, currentVersion);
			if (diff > 0) status = 'future';
			else if (diff === 0) status = 'current';
		}

		let isOpen = (status === 'future' || status === 'current') ? 'open' : '';
		let badgeHtml = getStatusBadge(status);

		html += `
        <details ${isOpen} class="group-block ${status}">
            <summary class="group-header">
                <div class="g-title">
                    <span style="font-family: monospace;">${groupId}</span>
                    ${badgeHtml}
                </div>
            </summary>
            <div class="group-body">
        `;

		if (group.patches) {
			group.patches.forEach(patch => {
				let summaryText = cleanString(patch.tagSummary || patch.tag);
				let detailsText = cleanString(patch.tagDetails || "");

				html += `
                <div class="patch-block">
                    <div class="patch-header">
                        <div class="p-title-row">
                            <span class="p-tag-chip">${patch.tag}</span>
                            <span class="p-date">${patch.date}</span>
                        </div>
                        <div class="p-summary">${formatContent(summaryText)}</div>
                        ${detailsText ? `<div class="p-details">${formatContent(detailsText)}</div>` : ''}
                    </div>

                    <div class="commit-list">
                `;

				if (patch.commits && patch.commits.length > 0) {
					patch.commits.forEach(c => {
						let type = c.type || 'legacy';
						let safeSummary = formatContent(c.summary);
						let safeDetails = c.details ? formatContent(c.details) : '';

						html += `
                        <div class="commit-row">
                            <span class="c-type ${type}">${type}</span>
                            <span class="c-hash">${c.hash.substring(0,7)}</span>
                            <div class="c-content">
                                <div class="c-subject">${safeSummary}</div>
                                ${safeDetails ? `<div class="c-body">${safeDetails}</div>` : ''}
                            </div>
                        </div>`;
					});
				} else {
					html += `<div style="color:#999; padding:10px;">No commits recorded.</div>`;
				}

				html += `</div></div>`;
			});
		}

		html += `</div></details>`;
	});

	html += `</div>`;
	return html;
}

// --- Helpers ---

function cleanString(str) {
	if (!str) return "";
	let res = str;
	if (res.startsWith("'") && res.endsWith("'")) res = res.substring(1, res.length - 1);
	if (res.startsWith('"') && res.endsWith('"')) res = res.substring(1, res.length - 1);
	res = res.replace(/\\"/g, '"');
	return res.trim();
}

function formatContent(text) {
	if (!text) return "";

	// 1. HTML 转义
	let safe = text
		.replace(/&/g, "&amp;")
		.replace(/</g, "&lt;")
		.replace(/>/g, "&gt;");

	// 2. 代码块解析 (```code```)
	safe = safe.replace(/```([\s\S]*?)```/g, `<div class="code-block">$1</div>`);

	// 3. 行内代码解析 (`code`)
	safe = safe.replace(/`([^`]+)`/g, '<code class="inline-code">$1</code>');

	// 4. 标题加粗
	safe = safe.replace(/^#+\s+(.*)$/gm, '<strong>$1</strong>');

	// ❌❌❌ 请删除或注释掉下面这一行 ❌❌❌
	// safe = safe.replace(/\n/g, '<br>');

	// 因为 CSS 里的 white-space: pre-wrap 会自动识别 \n 并处理空格

	return safe;
}

function compareVersions(v1, v2) {
	if (!v1 || !v2) return 0;
	let cleanV1 = v1.replace(/[^\d.]/g, '');
	let cleanV2 = v2.replace(/[^\d.]/g, '');
	let a = cleanV1.split('.').map(n => parseInt(n));
	let b = cleanV2.split('.').map(n => parseInt(n));
	for (let i = 0; i < Math.max(a.length, b.length); i++) {
		let val1 = a[i] || 0;
		let val2 = b[i] || 0;
		if (val1 > val2) return 1;
		if (val1 < val2) return -1;
	}
	return 0;
}

function getStatusBadge(status) {
	if (status === 'future') return '<span class="badge future">DEV</span>';
	if (status === 'current') return '<span class="badge current">CURRENT</span>';
	return '';
}

window.renderChangelog = renderChangelog;
