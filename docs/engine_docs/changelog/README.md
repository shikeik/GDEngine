# 版本更新日志

<!-- 挂载点 -->
<div id="changelog-app">
  <div class="loading">正在分析版本历史...</div>
</div>

<!-- 样式定义 -->
<style>
/* 容器 */
.changelog-container { font-family: 'Segoe UI', sans-serif; max-width: 800px; }
.log-meta { color: #666; font-size: 0.85em; margin-bottom: 20px; text-align: right; }

/* 版本块 */
.version-block { 
    margin-bottom: 15px; 
    border: 1px solid rgba(255,255,255,0.1); 
    border-radius: 6px; 
    background: rgba(255,255,255,0.02);
    transition: all 0.2s;
}
/* 状态色 */
.version-block.current { border: 1px solid #00eaff; box-shadow: 0 0 15px rgba(0, 234, 255, 0.1); }
.version-block.future { border: 1px dashed #ffcc00; opacity: 0.8; }
.version-block.history { border-color: #333; }

/* 标题栏 */
.version-header { 
    padding: 12px 15px; 
    cursor: pointer; 
    display: flex; 
    justify-content: space-between; 
    align-items: center; 
    user-select: none;
    background: rgba(0,0,0,0.2);
    border-radius: 6px;
}
.version-block[open] .version-header {
    border-bottom: 1px solid rgba(255,255,255,0.05);
    border-bottom-left-radius: 0;
    border-bottom-right-radius: 0;
}

.v-title { display: flex; align-items: center; gap: 10px; }
.v-tag { font-size: 1.3em; font-weight: bold; color: #fff; }
.v-date { font-family: monospace; color: #888; font-size: 0.9em; }

/* 徽章 */
.badge { padding: 2px 8px; border-radius: 4px; font-size: 0.75em; font-weight: bold; color: #000; }
.badge.current { background: #00eaff; }
.badge.future { background: #ffcc00; }

/* 内容区 */
.version-body { padding: 15px; }
.type-section { margin-bottom: 15px; }
.type-label { 
    display: inline-block; 
    font-size: 0.75em; 
    font-weight: bold; 
    padding: 2px 6px; 
    border-radius: 3px; 
    margin-bottom: 8px;
    color: #000;
}
/* 类型配色 */
.feat { background: #4caf50; }
.fix { background: #f44336; color: white; }
.perf { background: #ff9800; }
.docs { background: #2196f3; }
.legacy { background: #607d8b; }

ul { list-style: none; padding-left: 5px; margin: 0; }
.commit-item { margin-bottom: 10px; padding-left: 10px; border-left: 2px solid rgba(255,255,255,0.1); }
.commit-head { display: flex; justify-content: space-between; align-items: baseline; }
.commit-summary { font-weight: 500; color: #ddd; }
.commit-hash { font-family: monospace; color: #444; font-size: 0.8em; margin-left: 10px; }
.commit-details { 
    margin-top: 4px; 
    font-size: 0.9em; 
    color: #999; 
    line-height: 1.4;
    background: rgba(0,0,0,0.2);
    padding: 5px;
    border-radius: 3px;
}
</style>

<!-- 逻辑加载 -->
<script>
  (function() {
      console.log(">>> [Changelog] Script Started"); // 探针 1

      // --- [核心修复] 版本号持久化逻辑 ---
      
      // 1. 尝试从 URL 获取
      const params = new URLSearchParams(window.location.search);
      let localVer = params.get('v');

      // 2. 如果 URL 里有，存入 SessionStorage (持久化)
      if (localVer) {
          sessionStorage.setItem('gd_local_version', localVer);
          console.log("[Changelog] Version cached:", localVer);
      } 
      // 3. 如果 URL 里没有，尝试从 Storage 取 (防止路由跳转丢失)
      else {
          localVer = sessionStorage.getItem('gd_local_version') || '0.0.0';
          console.log("[Changelog] Version restored from cache:", localVer);
      }

      const jsonUrl = '/changelog/changelog.json'; 
      const jsUrl = '/changelog/changelog.js';

      fetch(jsonUrl)
        .then(res => {
            console.log(">>> [Changelog] JSON Response:", res.status); // 探针 2
            if(!res.ok) throw new Error("JSON 404: " + res.status);
            return res.json();
        })
        .then(data => {
            console.log(">>> [Changelog] JSON Loaded, Versions:", data.versions.length); // 探针 3
            loadRenderer(data, localVer);
        })
        .catch(err => {
            console.error(">>> [Changelog] Error:", err);
            document.getElementById('changelog-app').innerHTML = 
                `<div style="color:red; padding:10px;">加载失败: ${err.message}</div>`;
        });

      function loadRenderer(data, ver) {
          if (window.renderChangelog) {
              console.log(">>> [Changelog] Renderer already exists, running...");
              render();
              return;
          }

          console.log(">>> [Changelog] Loading JS from:", jsUrl);
          let script = document.createElement('script');
          script.src = jsUrl;
          script.onload = () => {
              console.log(">>> [Changelog] JS Loaded successfully"); // 探针 4
              render();
          };
          script.onerror = (e) => {
              console.error(">>> [Changelog] JS Load Failed", e);
              document.getElementById('changelog-app').innerHTML = "脚本加载失败";
          };
          document.body.appendChild(script);

          function render() {
              try {
                  const html = window.renderChangelog(data, ver);
                  document.getElementById('changelog-app').innerHTML = html;
                  console.log(">>> [Changelog] Render Complete"); // 探针 5
              } catch (e) {
                  console.error(">>> [Changelog] Render Logic Error:", e);
                  document.getElementById('changelog-app').innerHTML = "渲染逻辑错误: " + e.message;
              }
          }
      }
  })();
</script>
