/**
 * GDEngine Changelog System v4.2 (Live Navigation Fix)
 */

(function(global) {
	'use strict';

	// ============================================================
	// [Module 1] Config
	// ============================================================
	const Config = {
		debug: true,
		// 定义顶部滚动的偏移量 (px)，留出呼吸空间
		scrollOffset: 20,
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
			json: 'changelog/changelog.json',
			js:   'changelog/changelog.js'
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
			// [修改] 使用正则替换首尾的引号 (单引号或双引号)，无论是否成对
			// 原逻辑: if (res.startsWith("'") && res.endsWith("'")) ...
			return str.replace(/^['"]|['"]$/g, '').trim();
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
			const isOpen = (status === 'current') ? 'open' : '';

			const badgeConfig = Config.theme.badges[status] || Config.theme.badges.history;
			const badgeHtml = badgeConfig.text ? `<span class="${badgeConfig.className}">${badgeConfig.text}</span>` : '';

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
		// 防抖计时器
		_hashTimer: null,

		run: function() {
			Logger.init();
			Logger.info("Application Started");
			const localVer = this.getLocalVersion();
			this.fetchData(localVer);
			this.setupListeners();
		},

		// [Fix 1] 监听 Hash 变化，实现不刷新跳转
		setupListeners: function() {
			window.addEventListener('hashchange', () => {
				// 使用防抖，防止 Docsify 内部路由冲突
				if (this._hashTimer) clearTimeout(this._hashTimer);
				this._hashTimer = setTimeout(() => {
					this.handleDeepLink();
				}, 100);
			});
		},

		getLocalVersion: function() {
			const params = new URLSearchParams(window.location.search);
			let ver = params.get('v');
			
			// 策略 1: 如果 URL 有，优先用 URL，并尝试写入所有可能的缓存
			if (ver) {
				try { sessionStorage.setItem('gd_local_version', ver); } catch(e){}
				try { localStorage.setItem('gd_local_version', ver); } catch(e){}
				// 如果是本地 file:// 协议，cookie 可能无效，但还是试一下
				document.cookie = `gd_local_version=${ver}; path=/; max-age=31536000`; 
				return ver;
			}
			
			// 策略 2: 如果 URL 没有，依次尝试 sessionStorage -> localStorage -> Cookie
			try {
				ver = sessionStorage.getItem('gd_local_version');
				if (ver) return ver;
			} catch(e){}

			try {
				ver = localStorage.getItem('gd_local_version');
				if (ver) return ver;
			} catch(e){}

			// 尝试从 Cookie 读取
			const match = document.cookie.match(new RegExp('(^| )gd_local_version=([^;]+)'));
			if (match) return match[2];

			// 策略 3: 实在没有，回退到默认
			return '0.0.0';
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

				// 首次加载也检查一次链接
				setTimeout(() => this.handleDeepLink(), 200);

			} catch (e) {
				Logger.error("Render Error: " + e.message);
				console.error(e);
			}
		},

		// [Fix 3] 滚动到 Group (红框)，高亮 Patch (蓝框)
		// [Fix 4] 自动收起其他组 (手风琴效果)
		handleDeepLink: function() {
			const hash = window.location.hash;
			const queryPart = hash.split('?')[1];
			if (!queryPart) return;

			const params = new URLSearchParams(queryPart);
			const targetTag = params.get('target');

			if (!targetTag) return;

			Logger.info("Deep linking to: " + targetTag);

			const elementId = `patch-${targetTag}`;
			const targetPatchEl = document.getElementById(elementId);

			if (targetPatchEl) {
				// 2. 向上寻找所在的 Group (红框)
				let groupEl = targetPatchEl.closest('.group-block');

				// [New] 遍历所有 Group，除了目标组，其他的全部收起
				const allGroups = document.querySelectorAll('details.group-block');
				allGroups.forEach(g => {
					if (g !== groupEl) {
						g.open = false;
					}
				});

				// 3. 确保目标 Group 展开
				if (groupEl && groupEl.tagName.toLowerCase() === 'details') {
					groupEl.open = true;
				} else {
					// 兜底逻辑
					let p = targetPatchEl.parentElement;
					while (p) {
						if (p.tagName.toLowerCase() === 'details') {
							p.open = true;
							groupEl = p; // 修正引用
							// 同样要关掉其他的
							allGroups.forEach(g => { if(g !== p) g.open = false; });
							break;
						}
						p = p.parentElement;
					}
				}

				// 4. 执行定位与高亮
				setTimeout(() => {
					// A. 滚动目标：优先使用 Group (红框)
					const scrollTarget = groupEl || targetPatchEl;

					const rect = scrollTarget.getBoundingClientRect();
					const scrollTop = window.pageYOffset || document.documentElement.scrollTop;

					const finalY = scrollTop + rect.top - Config.scrollOffset;

					window.scrollTo({
						top: finalY,
						behavior: 'smooth'
					});

					// B. 高亮目标：Patch (蓝框)
					targetPatchEl.classList.remove('target-highlight');
					void targetPatchEl.offsetWidth;
					targetPatchEl.classList.add('target-highlight');

				}, 100);
			} else {
				Logger.info("Target element not found: " + elementId);
			}
		}
	};

	global.GDEngineChangelog = App;
	App.run();

})(window);
