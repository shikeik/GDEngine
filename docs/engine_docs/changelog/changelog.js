/**
 * GDEngine Changelog Renderer
 * 职责: 读取 JSON -> 比较版本 -> 生成 HTML
 */

function renderChangelog(data, currentVersion) {
	let html = `<div class="changelog-container">`;
	let updateTime = data.lastUpdated || "Unknown";

	html += `<div class="log-meta">最后更新: ${updateTime} | 当前本地版本: ${currentVersion}</div>`;

	// 遍历所有版本
	data.versions.forEach(ver => {
		// 1. 判定时空状态
		let status = 'history'; // 默认历史

		if (ver.id === "Dev-Build") {
			status = 'future';
		} else {
			let diff = compareVersions(ver.id, currentVersion);
			if (diff > 0) status = 'future';
			else if (diff === 0) status = 'current';
			else status = 'history';
		}

		// 2. 状态样式处理
		let isOpen = (status === 'current' || status === 'future') ? 'open' : ''; // 当前和未来版本默认展开，方便看新东西？
		// 或者：严格按照您的需求 -> 只有 Current 展开
		if (status === 'future') isOpen = '';
		if (status === 'current') isOpen = 'open';

		let badgeHtml = getStatusBadge(status);

		// 3. 构建 HTML 结构
		html += `
        <details ${isOpen} class="version-block ${status}">
            <summary class="version-header">
                <div class="v-title">
                    <span class="v-tag">${ver.tag}</span>
                    ${badgeHtml}
                </div>
                <span class="v-date">${ver.date}</span>
            </summary>
            <div class="version-body">
        `;

		// 4. 渲染提交列表
		if (!ver.commits || ver.commits.length === 0) {
			html += `<div class="empty-log">暂无详细记录</div>`;
		} else {
			// 按类型分组 (feat, fix...)
			let groups = groupCommits(ver.commits);

			// 优先渲染顺序
			const typeOrder = ['feat', 'fix', 'perf', 'refactor', 'docs', 'chore', 'test', 'legacy'];

			typeOrder.forEach(type => {
				if (groups[type]) {
					html += `<div class="type-section">
                                <span class="type-label ${type}">${type.toUpperCase()}</span>
                             <ul>`;
					groups[type].forEach(c => {
						// [核心修改] 使用 formatContent 处理文本
						// 1. 转义 HTML 标签 (防止 <script> 消失)
						// 2. 解析反引号代码块
						// 3. 处理换行
						let safeSummary = formatContent(c.summary);
						let safeDetails = c.details ? formatContent(c.details) : '';

						html += `
                        <li class="commit-item">
                            <div class="commit-head">
                                <span class="commit-summary">${c.summary}</span>
                                <span class="commit-hash">${c.hash.substring(0,7)}</span>
                            </div>
                            ${safeDetails ? `<div class="commit-details">${safeDetails}</div>` : ''}
                        </li>`;
					});
					html += `</ul></div>`;
				}
			});
		}

		html += `</div></details>`;
	});

	html += `</div>`;
	return html;
}

// --- [核心修复] 内容格式化工具 v2.0 ---
function formatContent(text) {
	if (!text) return "";

	// 1. HTML 转义 (安全第一)
	let safe = text
		.replace(/&/g, "&amp;")
		.replace(/</g, "&lt;")
		.replace(/>/g, "&gt;")
		.replace(/"/g, "&quot;")
		.replace(/'/g, "&#039;");

	// 2. 处理代码块 (```code```) - 优先级最高
	// 匹配 ``` ... ``` (包括换行符)
	safe = safe.replace(/```([\s\S]*?)```/g, function(match, code) {
		// 既然是代码块，去掉首尾可能多余的换行，并包在 div 里
		return `<div class="code-block">${code.trim()}</div>`;
	});

	// 3. 处理行内代码 (`code`) - 优先级次之
	// 匹配 ` ... `
	safe = safe.replace(/`([^`]+)`/g, '<code class="inline-code">$1</code>');

	// 4. [移除] 不再手动替换 \n 为 <br>
	// 因为 CSS 里的 white-space: pre-wrap 会自动处理

	return safe;
}

// --- Helpers ---

// 版本比较: v1 > v2 返回 1, v1 < v2 返回 -1, 相等返回 0
function compareVersions(v1, v2) {
	if (!v1 || !v2) return 0;
	// 移除可能存在的非数字后缀 (如 -beta) 简单处理
	let cleanV1 = v1.replace(/-.*/, '');
	let cleanV2 = v2.replace(/-.*/, '');

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
	if (status === 'future') return '<span class="badge future">🚀 预览 (Preview)</span>';
	if (status === 'current') return '<span class="badge current">✅ 当前 (Installed)</span>';
	return '';
}

function groupCommits(commits) {
	let groups = {};
	commits.forEach(c => {
		let t = c.type || 'legacy';
		if (!groups[t]) groups[t] = [];
		groups[t].push(c);
	});
	return groups;
}

// 导出到全局
window.renderChangelog = renderChangelog;
