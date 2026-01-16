# 版本更新日志

<div id="changelog-app">
	<div class="loading">正在初始化...</div>
</div>

<details style="margin-top:30px; border:1px solid #eee; background:#fafafa; border-radius:4px;">
	<summary style="padding:8px; cursor:pointer; color:#999; font-size:0.8em; font-family:monospace;">🛠️ Debug Console</summary>
	<div id="debug-output" style="padding:10px; background:#222; color:#0f0; font-family:Consolas; font-size:12px; height:100px; overflow-y:auto; white-space:pre-wrap;"></div>
</details>

<style>
	:root {
		--bg-panel: #FFFFFF;
		--border-color: #E5E5E5;
		--accent-teal: #09D2B8;
	}

	.changelog-container {
		max-width: 100%;
		padding-top: 10px;
		font-family: "Segoe UI", "Inter", sans-serif;
	}

	.log-meta {
		font-size: 0.85em;
		color: #888;
		border-bottom: 2px solid var(--accent-teal);
		padding-bottom: 10px;
		margin-bottom: 25px;
		font-weight: 600;
	}

	/* Group Block */
	.group-block {
		margin-bottom: 15px;
		border: 1px solid var(--border-color);
		background: #FAFAFA;
		border-radius: 4px;
	}

	.group-header {
		padding: 10px 15px;
		cursor: pointer;
		background: #F5F5F5;
		display: flex;
		align-items: center;
		border-left: 4px solid transparent;
	}
	.group-header:hover { background: #EEE; }

	.group-block[open] .group-header {
		background: #E8E8E8;
		border-left: 4px solid var(--accent-teal);
		border-bottom: 1px solid var(--border-color);
	}

	.g-title { font-size: 1.1em; font-weight: bold; color: #333; display: flex; align-items: center; gap: 10px; }

	/* Badges */
	.badge { padding: 2px 8px; border-radius: 10px; font-size: 0.75em; color: white; font-weight:normal; }
	.badge.current { background: var(--accent-teal); }
	.badge.future { background: #FBC02D; color: #333; }

	/* Content */
	.group-body { padding: 0; background: #FFF; }

	.patch-block {
		padding: 20px;
		border-bottom: 1px solid #F0F0F0;
	}
	.patch-block:last-child { border-bottom: none; }

	.p-tag-chip { font-size: 1.3em; font-weight: 700; color: #222; margin-right: 10px; }
	.p-date { color: #999; font-size: 0.9em; font-family: Consolas, monospace; }

	.p-summary { font-size: 1.1em; color: #222; margin: 8px 0; font-weight: 500; line-height: 1.5; }

	/* [修改] 移除 pre-wrap，依靠 <br> */
	.p-details {
		/* ...之前的样式保持不变... */
		background: #F9F9F9;
		padding: 10px 15px;
		border-left: 3px solid #DDD;
		/* ↓↓↓ 请添加或确保有这一行 ↓↓↓ */
		white-space: pre-wrap;
		font-family: Consolas, "Segoe UI", sans-serif; /* 可选：加上等宽字体对齐更整齐 */
	}

	/* Commits */
	.commit-row {
		display: flex;
		align-items: baseline;
		gap: 12px;
		padding: 6px 0;
		border-bottom: 1px dashed #F0F0F0;
	}
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

	.c-hash { color: #CCC; font-family: Consolas, monospace; font-size: 0.85em; }
	.c-content { flex: 1; }
	.c-subject { color: #444; font-size: 0.95em; }

	.c-body {
		/* ...之前的样式保持不变... */
		font-size: 0.85em;
		color: #888;
		margin-top: 4px;
		/* ↓↓↓ 请添加或确保有这一行 ↓↓↓ */
		white-space: pre-wrap;
	}

	/* [核心修复] 单行代码样式 - 强制覆盖 Docsify 默认样式 */
	.inline-code {
		background-color: #F3F4F4 !important; /* 浅灰背景 */
		color: #C7254E !important;            /* 玫红文字 */
		border: 1px solid #E8E8E8 !important;
		padding: 2px 5px !important;
		border-radius: 3px !important;
		font-family: Consolas, monospace !important;
		font-size: 0.9em !important;
	}

	.code-block {
		background: #F8F8F8;
		border: 1px solid #EEE;
		padding: 10px;
		margin: 8px 0;
		border-radius: 4px;
		font-family: Consolas, monospace;
		color: #333;
		overflow-x: auto;
	}
</style>

<script>
	(function() {
		function logProbe(msg) {
			console.log(msg);
			const out = document.getElementById('debug-output');
			if(out) out.innerText += msg + "\n";
		}

		logProbe(">>> Init Script");

		const params = new URLSearchParams(window.location.search);
		let localVer = params.get('v');
		if (localVer) sessionStorage.setItem('gd_local_version', localVer);
		else localVer = sessionStorage.getItem('gd_local_version') || '0.0.0';

		// [Key Fix] 使用绝对路径
		const jsonUrl = '/changelog/changelog.json';
		const jsUrl = '/changelog/changelog.js';

		logProbe(">>> Fetching: " + jsonUrl);

		fetch(jsonUrl)
			.then(res => {
				if(!res.ok) throw new Error("HTTP " + res.status);
				return res.json();
			})
			.then(data => {
				logProbe(">>> Data Loaded");
				loadRenderer(data, localVer);
			})
			.catch(err => {
				logProbe("!!! Error: " + err.message);
				document.getElementById('changelog-app').innerHTML = `<div style="color:red;">Error: ${err.message}</div>`;
			});

		function loadRenderer(data, ver) {
			if (window.renderChangelog) {
				doRender(data, ver);
				return;
			}
			let script = document.createElement('script');
			script.src = jsUrl;
			script.onload = () => doRender(data, ver);
			script.onerror = () => logProbe("!!! Script Load Failed");
			document.body.appendChild(script);
		}

		function doRender(data, ver) {
			try {
				document.getElementById('changelog-app').innerHTML = window.renderChangelog(data, ver);
				logProbe(">>> Render Success");
			} catch(e) {
				logProbe("!!! Render Error: " + e.message);
			}
		}
	})();
</script>
