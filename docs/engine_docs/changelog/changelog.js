/**
 * GDEngine Changelog System v4.0 (Refactored)
 * 架构: MVC (Config -> Model/Utils -> View/Renderer)
 * 职责: 自动化解析 Gradle 生成的 changelog.json 并渲染为 Unity 风格页面
 */

(function(global) {
	'use strict';

	// ============================================================
	// [Module 1] Config - 全局配置层
	// 修改此处即可改变颜色、文本映射，无需触碰逻辑代码
	// ============================================================
	const Config = {
		debug: true, // 是否输出探针日志

		// CSS 类名映射
		theme: {
			badges: {
				future:  { text: "DEV / PREVIEW", className: "badge future" },
				current: { text: "CURRENT",       className: "badge current" },
				history: { text: "",              className: "" } // 历史版本不显示 Badge
			},
			// Commit 类型对应的 CSS 类名
			types: {
				feat:     'feat',
				fix:      'fix',
				perf:     'perf',
				docs:     'docs',
				chore:    'chore',
				refactor: 'refactor',
				test:     'chore', // test 归类为灰色
				legacy:   'chore'  // 未知归类为灰色
			}
		},

		// 资源路径 (必须是绝对路径)
		paths: {
			json: '/changelog/changelog.json',
			js:   '/changelog/changelog.js' // 用于自检
		}
	};

	// ============================================================
	// [Module 2] Logger - 探针系统
	// 统一输出到 Console 和 页面的 Debug 面板
	// ============================================================
	const Logger = {
		_uiOutput: null,

		init: function() {
			this._uiOutput = document.getElementById('debug-output');
		},

		info: function(msg) {
			if (!Config.debug) return;
			const fmtMsg = `[GD-Log] ${msg}`;
			console.log(fmtMsg);
			if (this._uiOutput) {
				this._uiOutput.innerText += `> ${msg}\n`;
			}
		},

		error: function(msg) {
			const fmtMsg = `[GD-Err] ${msg}`;
			console.error(fmtMsg);
			if (this._uiOutput) {
				this._uiOutput.innerHTML += `<span style="color:#ff5555;">! ${msg}</span>\n`;
			}
		}
	};

	// ============================================================
	// [Module 3] Utils - 工具层 (纯函数)
	// 处理字符串清洗、版本比较、格式转换
	// ============================================================
	const Utils = {
		/**
		 * 清洗 Gradle 生成的脏字符串
		 * 去除首尾的单引号、双引号，处理转义符
		 */
		cleanString: function(str) {
			if (!str) return "";
			let res = str;
			// 剥离外层引号
			if (res.startsWith("'") && res.endsWith("'")) res = res.substring(1, res.length - 1);
			if (res.startsWith('"') && res.endsWith('"')) res = res.substring(1, res.length - 1);
			// 修复转义
			res = res.replace(/\\"/g, '"');
			return res.trim();
		},

		/**
		 * 格式化内容文本
		 * 1. HTML 转义 (安全)
		 * 2. Markdown 简单解析 (代码块、加粗)
		 * 3. 注意: 不替换 \n 为 <br>，依赖 CSS pre-wrap 保持缩进
		 */
		formatMarkdown: function(text) {
			if (!text) return "";

			// 1. HTML Escaping
			let safe = text
				.replace(/&/g, "&amp;")
				.replace(/</g, "&lt;")
				.replace(/>/g, "&gt;");

			// 2. Code Blocks (```...```)
			safe = safe.replace(/```([\s\S]*?)```/g, `<div class="code-block">$1</div>`);

			// 3. Inline Code (`...`)
			safe = safe.replace(/`([^`]+)`/g, '<code class="inline-code">$1</code>');

			// 4. Bold Titles (# Title)
			safe = safe.replace(/^#+\s+(.*)$/gm, '<strong>$1</strong>');

			return safe;
		},

		/**
		 * 版本号比较
		 * @returns 1 if v1 > v2, -1 if v1 < v2, 0 if equal
		 */
		compareVersions: function(v1, v2) {
			if (!v1 || !v2) return 0;
			// 只保留数字和点
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

		/**
		 * 获取版本状态 (Future/Current/History)
		 */
		getVersionStatus: function(versionId, localVersion) {
			if (versionId === "In Development") return 'future';

			const diff = this.compareVersions(versionId, localVersion);
			if (diff > 0) return 'future';
			if (diff === 0) return 'current';
			return 'history';
		}
	};

	// ============================================================
	// [Module 4] Renderer - 视图层
	// 负责拼接 HTML 字符串，单一职责
	// ============================================================
	const Renderer = {

		/** 主入口 */
		renderAll: function(data, localVer) {
			Logger.info(`Rendering View... (Local Ver: ${localVer})`);

			if (!data.groups) return `<div class="empty-log">Error: Invalid Data Structure</div>`;

			let html = `<div class="changelog-container">`;

			// Header Meta
			const updateTime = data.lastUpdated || "Unknown";
			html += `
				<div class="log-meta">
					Last Updated: ${updateTime}
					&nbsp;|&nbsp;
					Local Engine: <span class="meta-version">${localVer}</span>
				</div>`;

			// Render Groups
			data.groups.forEach(group => {
				html += this.renderGroup(group, localVer);
			});

			html += `</div>`;
			return html;
		},

		/** 渲染大版本组 (Level 1) */
		renderGroup: function(group, localVer) {
			const groupId = group.id;
			const status = Utils.getVersionStatus(groupId, localVer);

			// 只有 Current 默认展开
			const isOpen = (status === 'current') ? 'open' : '';

			// 获取 Badge 配置
			const badgeConfig = Config.theme.badges[status] || Config.theme.badges.history;
			const badgeHtml = badgeConfig.text
				? `<span class="${badgeConfig.className}">${badgeConfig.text}</span>`
				: '';

			let html = `
			<details ${isOpen} class="group-block ${status}">
				<summary class="group-header">
					<div class="g-title">
						<span style="font-family: monospace;">${groupId}</span>
						${badgeHtml}
					</div>
				</summary>
				<div class="group-body">`;

			// Render Patches inside Group
			if (group.patches) {
				group.patches.forEach(patch => {
					html += this.renderPatch(patch);
				});
			}

			html += `</div></details>`;
			return html;
		},

		/** 渲染补丁/Tag (Level 2) */
		renderPatch: function(patch) {
			const summary = Utils.cleanString(patch.tagSummary || patch.tag);
			const details = Utils.cleanString(patch.tagDetails || "");

			let html = `
			<div class="patch-block">
				<div class="patch-header">
					<div class="p-title-row">
						<span class="p-tag-chip">${patch.tag}</span>
						<span class="p-date">${patch.date}</span>
					</div>
					<div class="p-summary">${Utils.formatMarkdown(summary)}</div>
					${details ? `<div class="p-details">${Utils.formatMarkdown(details)}</div>` : ''}
				</div>
				<div class="commit-list">`;

			// Render Commits
			if (patch.commits && patch.commits.length > 0) {
				patch.commits.forEach(c => {
					html += this.renderCommit(c);
				});
			} else {
				html += `<div class="empty-commits">No commits recorded.</div>`;
			}

			html += `</div></div>`;
			return html;
		},

		/** 渲染单行 Commit (Level 3) */
		renderCommit: function(commit) {
			// 确定类型样式
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
	// [Module 5] App - 控制层
	// 初始化、数据获取、错误处理
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
				Logger.info("Version from URL: " + ver);
			} else {
				ver = sessionStorage.getItem('gd_local_version') || '0.0.0';
				Logger.info("Version from Cache: " + ver);
			}
			return ver;
		},

		fetchData: function(localVer) {
			Logger.info("Fetching: " + Config.paths.json);

			fetch(Config.paths.json)
				.then(res => {
					if (!res.ok) throw new Error("HTTP " + res.status);
					return res.json();
				})
				.then(data => {
					Logger.info("Data Loaded. Group Count: " + (data.groups ? data.groups.length : 0));
					this.mount(data, localVer);
				})
				.catch(err => {
					Logger.error("Fetch Failed: " + err.message);
					document.getElementById('changelog-app').innerHTML =
						`<div style="color:#ff5555; padding:20px;">
							<h3>Failed to load changelog</h3>
							<p>${err.message}</p>
						</div>`;
				});
		},

		mount: function(data, localVer) {
			try {
				const html = Renderer.renderAll(data, localVer);
				document.getElementById('changelog-app').innerHTML = html;
				Logger.info("DOM Updated Successfully");
			} catch (e) {
				Logger.error("Render Error: " + e.message);
				console.error(e);
			}
		}
	};

	// 暴露全局入口 (供 HTML 调用)
	global.GDEngineChangelog = App;

	// 自动启动
	App.run();

})(window);
