/**
 * GDEngine Changelog System v4.1 (Deep Linking Support)
 */

(function(global) {
	'use strict';

	// ============================================================
	// [Module 1] Config
	// ============================================================
	const Config = {
		debug: true,
		theme: {
			badges: {
				dev:     { text: "DEV",     className: "badge dev" },
				preview: { text: "PREVIEW", className: "badge preview" },
				current: { text: "CURRENT", className: "badge current" },
				history: { text: "",        className: "" }
			},
			types: {
				feat: 'feat', fix: 'fix', perf: 'perf', docs: 'docs',
				chore: 'chore', refactor: 'refactor', test: 'chore', legacy: 'chore'
			}
		},
		paths: {
			json: '/changelog/changelog.json',
			js:   '/changelog/changelog.js'
		}
	};

	// ============================================================
	// [Module 2] Logger
	// ============================================================
	const Logger = {
		_uiOutput: null,
		init: function() { this._uiOutput = document.getElementById('debug-output'); },
		info: function(msg) {
			if (!Config.debug) return;
			console.log(`[GD-Log] ${msg}`);
			if (this._uiOutput) this._uiOutput.innerText += `> ${msg}\n`;
		},
		error: function(msg) {
			console.error(`[GD-Err] ${msg}`);
			if (this._uiOutput) this._uiOutput.innerHTML += `<span style="color:#ff5555;">! ${msg}</span>\n`;
		}
	};

	// ============================================================
	// [Module 3] Utils
	// ============================================================
	const Utils = {
		cleanString: function(str) {
			if (!str) return "";
			let res = str;
			if (res.startsWith("'") && res.endsWith("'")) res = res.substring(1, res.length - 1);
			if (res.startsWith('"') && res.endsWith('"')) res = res.substring(1, res.length - 1);
			res = res.replace(/\\"/g, '"');
			return res.trim();
		},
		formatMarkdown: function(text) {
			if (!text) return "";
			let safe = text.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;");
			safe = safe.replace(/```([\s\S]*?)```/g, `<div class="code-block">$1</div>`);
			safe = safe.replace(/`([^`]+)`/g, '<code class="inline-code">$1</code>');
			safe = safe.replace(/^#+\s+(.*)$/gm, '<strong>$1</strong>');
			return safe;
		},
		compareVersions: function(v1, v2) {
			if (!v1 || !v2) return 0;
			const clean = (v) => v.replace(/[^\d.]/g, '');
			const a = clean(v1).split('.').map(n => parseInt(n));
			const b = clean(v2).split('.').map(n => parseInt(n));
			for (let i = 0; i < Math.max(a.length, b.length); i++) {
				const val1 = a[i] || 0;
				const val2 = b[i] || 0;
				if (val1 > val2) return 1;
				if (val1 < val2) return -1;
			}
			return 0;
		},
		getVersionStatus: function(versionId, localVersion) {
			if (versionId === "In Development") return 'dev';
			const diff = this.compareVersions(versionId, localVersion);
			if (diff > 0) return 'preview';
			if (diff === 0) return 'current';
			return 'history';
		}
	};

	// ============================================================
	// [Module 4] Renderer
	// ============================================================
	const Renderer = {
		renderAll: function(data, localVer) {
			Logger.info(`Rendering View... (Local Ver: ${localVer})`);
			if (!data.groups) return `<div class="empty-log">Error: Invalid Data Structure</div>`;

			let html = `<div class="changelog-container">`;
			const updateTime = data.lastUpdated || "Unknown";
			html += `<div class="log-meta">Last Updated: ${updateTime} &nbsp;|&nbsp; Local Engine: <span class="meta-version">${localVer}</span></div>`;

			data.groups.forEach(group => {
				html += this.renderGroup(group, localVer);
			});

			html += `</div>`;
			return html;
		},

		renderGroup: function(group, localVer) {
			const groupId = group.id;
			const status = Utils.getVersionStatus(groupId, localVer);
			const isOpen = (status === 'current') ? 'open' : ''; // 只有当前版本默认展开

			const badgeConfig = Config.theme.badges[status] || Config.theme.badges.history;
			const badgeHtml = badgeConfig.text ? `<span class="${badgeConfig.className}">${badgeConfig.text}</span>` : '';

			// [Update] 添加 ID: group-{id}
			let html = `
			<details ${isOpen} id="group-${groupId}" class="group-block ${status}">
				<summary class="group-header">
					<div class="g-title">
						<span style="font-family: monospace;">${groupId}</span>
						${badgeHtml}
					</div>
				</summary>
				<div class="group-body">`;

			if (group.patches) {
				group.patches.forEach(patch => {
					html += this.renderPatch(patch);
				});
			}
			html += `</div></details>`;
			return html;
		},

		renderPatch: function(patch) {
			const summary = Utils.cleanString(patch.tagSummary || patch.tag);
			const details = Utils.cleanString(patch.tagDetails || "");

			// [Update] 添加 ID: patch-{tag}
			let html = `
			<div id="patch-${patch.tag}" class="patch-block">
				<div class="patch-header">
					<div class="p-title-row">
						<span class="p-tag-chip">${patch.tag}</span>
						<span class="p-date">${patch.date}</span>
					</div>
					<div class="p-summary">${Utils.formatMarkdown(summary)}</div>
					${details ? `<div class="p-details">${Utils.formatMarkdown(details)}</div>` : ''}
				</div>
				<div class="commit-list">`;

			if (patch.commits && patch.commits.length > 0) {
				patch.commits.forEach(c => html += this.renderCommit(c));
			} else {
				html += `<div class="empty-commits">No commits recorded.</div>`;
			}
			html += `</div></div>`;
			return html;
		},

		renderCommit: function(commit) {
			const rawType = commit.type || 'legacy';
			const typeClass = Config.theme.types[rawType] || 'chore';
			const summary = Utils.formatMarkdown(commit.summary);
			const details = commit.details ? Utils.formatMarkdown(commit.details) : '';
			const shortHash = commit.hash.substring(0, 7);

			return `
			<div class="commit-row">
				<span class="c-type ${typeClass}">${rawType}</span>
				<span class="c-hash">${shortHash}</span>
				<div class="c-content">
					<div class="c-subject">${summary}</div>
					${details ? `<div class="c-body">${details}</div>` : ''}
				</div>
			</div>`;
		}
	};

	// ============================================================
	// [Module 5] App
	// ============================================================
	const App = {
		run: function() {
			Logger.init();
			Logger.info("Application Started");
			const localVer = this.getLocalVersion();
			this.fetchData(localVer);
		},

		getLocalVersion: function() {
			const params = new URLSearchParams(window.location.search);
			let ver = params.get('v');
			if (ver) {
				sessionStorage.setItem('gd_local_version', ver);
			} else {
				ver = sessionStorage.getItem('gd_local_version') || '0.0.0';
			}
			return ver;
		},

		fetchData: function(localVer) {
			fetch(Config.paths.json)
				.then(res => {
					if (!res.ok) throw new Error("HTTP " + res.status);
					return res.json();
				})
				.then(data => {
					this.mount(data, localVer);
				})
				.catch(err => {
					Logger.error("Fetch Failed: " + err.message);
					document.getElementById('changelog-app').innerHTML = `<div style="color:red;padding:20px;">Load Failed: ${err.message}</div>`;
				});
		},

		mount: function(data, localVer) {
			try {
				const html = Renderer.renderAll(data, localVer);
				document.getElementById('changelog-app').innerHTML = html;
				Logger.info("DOM Updated");

				// [New] 渲染完成后处理深度链接
				this.handleDeepLink();

			} catch (e) {
				Logger.error("Render Error: " + e.message);
				console.error(e);
			}
		},

		// [New] 处理 ?target=... 参数
		handleDeepLink: function() {
			// 1. 获取 Target (从 Hash Query 获取)
			// Docsify URL: #/changelog/README?target=v1.10.12.0
			const hash = window.location.hash;
			const queryPart = hash.split('?')[1];
			if (!queryPart) return;

			const params = new URLSearchParams(queryPart);
			const targetTag = params.get('target');

			if (!targetTag) return;

			Logger.info("Deep linking to: " + targetTag);

			// 2. 查找 DOM 元素
			const elementId = `patch-${targetTag}`;
			const targetEl = document.getElementById(elementId);

			if (targetEl) {
				// 3. 展开父级 (找到最近的 details)
				let parent = targetEl.parentElement;
				while (parent) {
					if (parent.tagName.toLowerCase() === 'details') {
						parent.open = true;
						break;
					}
					parent = parent.parentElement;
				}

				// 4. 滚动与高亮
				setTimeout(() => {
					targetEl.scrollIntoView({ behavior: 'smooth', block: 'center' });
					targetEl.classList.add('target-highlight');
					// 动画结束后移除类 (可选)
					// setTimeout(() => targetEl.classList.remove('target-highlight'), 2000);
				}, 100); // 延时一小会儿确保 DOM 稳定
			} else {
				Logger.info("Target element not found: " + elementId);
			}
		}
	};

	global.GDEngineChangelog = App;
	App.run();

})(window);
