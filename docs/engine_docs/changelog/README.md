# 版本更新日志

<div id="changelog-app">
	<div class="loading">正在初始化文档引擎...</div>
</div>

<!-- 可视化探针控制台 -->
<details style="margin-top:30px; border:1px solid #e0e0e0; background:#f9f9f9; border-radius:4px;">
	<summary style="padding:8px; cursor:pointer; color:#999; font-size:0.8em; font-family:monospace;">
		🛠️ Debug Console
	</summary>
	<div id="debug-output" style="padding:10px; background:#2b2b2b; color:#ccc; font-family:Consolas,monospace; font-size:12px; height:100px; overflow-y:auto; white-space:pre-wrap;"></div>
</details>

<style>
	/* =========================================
	   Module: Theme Variables (Unity Light)
	   ========================================= */
	:root {
		--bg-page:      #FFFFFF;
		--bg-panel:     #FAFAFA;
		--bg-header:    #F5F5F5;
		--bg-hover:     #EFEFEF;
		--bg-code:      #F3F4F4;

		--border-light: #F0F0F0;
		--border-med:   #E5E5E5;
		--border-dark:  #DDDDDD;

		--accent-teal:  #09D2B8;
		--accent-warn:  #FBC02D;
		--accent-purple:#ca50d9;

		--text-main:    #333333;
		--text-sub:     #555555;
		--text-dim:     #999999;
		--text-code:    #C7254E;
	}

	/* =========================================
	   Module: Layout & Containers
	   ========================================= */
	.changelog-container {
		max-width: 100%;
		padding-top: 10px;
		font-family: "Segoe UI", "Inter", sans-serif;
		color: var(--text-main);
	}

	.log-meta {
		font-size: 0.85em;
		color: var(--text-dim);
		border-bottom: 2px solid var(--accent-teal);
		padding-bottom: 10px;
		margin-bottom: 25px;
		font-weight: 600;
	}

	.meta-version {
		color: white;
		background: var(--accent-teal);
		padding: 2px 6px;
		border-radius: 4px;
	}

	/* =========================================
	   Module: Group Block (Level 1)
	   ========================================= */
	.group-block {
		margin-bottom: 15px;
		border: 1px solid var(--border-med);
		background: var(--bg-panel);
		border-radius: 4px;
		box-shadow: 0 2px 5px rgba(0,0,0,0.02);
	}

	.group-header {
		padding: 10px 15px;
		cursor: pointer;
		background: var(--bg-header);
		display: flex;
		align-items: center;
		border-left: 4px solid transparent;
		transition: background 0.2s;
	}
	.group-header:hover { background: var(--bg-hover); }

	/* Active State */
	.group-block[open] .group-header {
		background: #E8E8E8;
		border-left: 4px solid var(--accent-teal);
		border-bottom: 1px solid var(--border-med);
	}

	.g-title {
		font-size: 1.1em;
		font-weight: bold;
		color: var(--text-main);
		display: flex;
		align-items: center;
		gap: 10px;
	}

	/* =========================================
	   Module: Badges
	   ========================================= */
	.badge {
		padding: 2px 8px;
		border-radius: 10px;
		font-size: 0.75em;
		color: white;
		font-weight: normal;
	}
	.badge.current { background: var(--accent-teal); }
	.badge.dev { background: var(--accent-purple); color: #FFF; }
	.badge.preview { background: var(--accent-warn); color: #333; }

	/* =========================================
	   Module: Patch Content (Level 2)
	   ========================================= */
	.group-body { padding: 0; background: #FFF; }

	.patch-block {
		padding: 20px;
		border-bottom: 1px solid var(--border-light);
	}
	.patch-block:last-child { border-bottom: none; }

	.p-tag-chip {
		font-size: 1.3em;
		font-weight: 700;
		color: #222;
		margin-right: 10px;
	}
	.p-date {
		color: var(--text-dim);
		font-size: 0.9em;
		font-family: Consolas, monospace;
	}

	.p-summary {
		background: #F0F0F0;
		font-size: 1.1em;
		color: #222;
		margin: 8px 0;
		font-weight: 500;
		line-height: 1.5;
	}

	/* Tag Details with Pre-Wrap */
	.p-details {
		/* [修改] 加深背景色，增加着重感 */
		background: #F9F9F9;
		padding: 10px 15px;
		border-left: 3px solid var(--border-dark);
		color: var(--text-sub);
		font-size: 0.95em;
		margin-bottom: 15px;
		white-space: pre-wrap; /* 关键：保留换行和缩进 */
		/* [修改] 移除 Consolas 强制等宽字体，改回普通字体以提升阅读体验 */
		font-family: "Segoe UI", "Inter", sans-serif;
		line-height: 1.6;
	}

	/* =========================================
	   Module: Commit List (Level 3)
	   ========================================= */
	.commit-row {
		display: flex;
		align-items: baseline;
		gap: 12px;
		padding: 5px 0;
		border-bottom: 1px dashed var(--border-light);
	}
	.commit-row:last-child { border-bottom: none; }

	/* Type Labels */
	.c-type {
		font-family: Consolas, monospace;
		font-size: 0.75em;
		padding: 2px 6px;
		border-radius: 3px;
		color: white;
		font-weight: bold;
		text-transform: uppercase;
		min-width: 55px;
		text-align: center;
	}
	.feat { background: #369947; }
	.fix { background: #D32F2F; }
	.perf { background: #F57C00; }
	.docs { background: #1976D2; }
	.chore { background: #607D8B; }
	.refactor { background: #7B1FA2; }

	.c-hash { color: #CCC; font-family: Consolas, monospace; font-size: 0.85em; }
	.c-content { flex: 1; }
	.c-subject { color: #444; font-size: 0.95em; }

	/* Commit Details with Pre-Wrap */
	.c-body {
		font-size: 0.85em;
		color: #888;
		margin-top: 4px;
		line-height: 1.5;
		white-space: pre-wrap; /* 关键：保留换行和缩进 */
	}

	.empty-commits { color: #BBB; font-style: italic; }

	/* =========================================
	   Module: Code Highlighting
	   ========================================= */
	.inline-code {
		background-color: var(--bg-code) !important;
		color: var(--text-code) !important;
		border: 1px solid #E8E8E8 !important;
		padding: 2px 5px !important;
		border-radius: 3px !important;
		font-family: Consolas, monospace !important;
		font-size: 0.9em !important;
	}

	.code-block {
		background: #F8F8F8;
		border: 1px solid var(--border-dark);
		padding: 10px;
		margin: 8px 0;
		border-radius: 4px;
		font-family: Consolas, monospace;
		color: #333;
		overflow-x: auto;
	}

	/* [New] Deep Link Highlighting */
	@keyframes targetFlash {
		0% { background-color: rgba(9, 210, 184, 0.3); box-shadow: 0 0 10px rgba(9, 210, 184, 0.3); }
		100% { background-color: transparent; box-shadow: none; }
	}

	.target-highlight {
		animation: targetFlash 2s ease-out forwards;
		border-left: 4px solid var(--accent-teal) !important; /* 强制加上左侧青条 */
	}
</style>

<!-- 启动引导脚本 -->
<script>
	(function() {
		// 1. 立即获取 Console DOM，保证一旦运行就能看到日志
		const debugOut = document.getElementById('debug-output');
		function logBoot(msg) {
			console.log(msg);
			if(debugOut) debugOut.innerText += msg + "\n";
		}

		logBoot(">>> [Boot] 引导脚本启动");

		// 2. 动态加载核心逻辑库
		const jsUrl = 'changelog/changelog.js'; // 相对路径

		// 防缓存策略 (可选，开发期很有用)
		const timestamp = new Date().getTime();
		const finalUrl = jsUrl + "?t=" + timestamp;

		logBoot(">>> [Boot] 请求核心库: " + finalUrl);

		let script = document.createElement('script');
		script.src = finalUrl;

		script.onload = () => {
			logBoot(">>> [Boot] 核心库加载成功，逻辑移交...");
			// changelog.js 内部有 App.run()，加载完会自动接管
		};

		script.onerror = (e) => {
			logBoot("!!! [Boot] 核心库加载失败 (404/Network Error)");
			document.getElementById('changelog-app').innerHTML =
				"<div style='color:red'>核心脚本加载失败，请检查路径: " + jsUrl + "</div>";
		};

		document.body.appendChild(script);
	})();
</script>
