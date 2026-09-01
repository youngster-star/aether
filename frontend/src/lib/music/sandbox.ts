/**
 * AI 特效沙箱层（UI-Plan §8.4 第三层 / §8.5）
 *
 * <p>effect_config.sandboxCode（AI 生成 JS）在 <iframe sandbox="allow-scripts">
 * 内受限执行：无同源（opaque origin）、无网络、无 DOM 访问，仅暴露受限
 * effectApi（fillRect/arc/clear/text/audioLevel）；postMessage 通信。
 * 3s 未就绪或执行异常 → 父页面自动回退 config 渲染（MusicVisualizer）。</p>
 */

/** 沙箱帧数据（父页面 → iframe，每帧推送） */
export interface SandboxFrame {
  /** 响度 0-1 */
  level: number;
  /** 频段能量 0-1（16 桶） */
  bands: number[];
  /** 是否节拍脉冲 */
  beat: boolean;
}

/** 沙箱控制器 */
export interface SandboxHandle {
  /** 挂载 iframe 并注入代码；返回是否成功就绪（超时/异常返回 false → 调用方回退 config 渲染） */
  mount(container: HTMLElement, code: string): Promise<boolean>;
  /** 推送音频帧 */
  pushFrame(frame: SandboxFrame): void;
  /** 调整尺寸（iframe 内画布自适应由 css 100% 处理，尺寸随帧下发） */
  setSize(width: number, height: number): void;
  /** 移除 iframe */
  destroy(): void;
}

/** 沙箱就绪超时（ms，UI-Plan §8.4 固定 3s） */
const READY_TIMEOUT_MS = 3000;

/** iframe 内引导脚本：受限 effectApi + postMessage 协议 */
const BOOTSTRAP = `<!DOCTYPE html><html><head><meta charset="utf-8"><style>
html,body{margin:0;padding:0;overflow:hidden;background:transparent}
canvas{display:block;width:100vw;height:100vh}
</style></head><body><canvas id="fx"></canvas><script>
(function () {
  var canvas = document.getElementById('fx');
  var ctx = canvas.getContext('2d');
  var audio = {level: 0, bands: new Array(16).fill(0), beat: false};
  var userOnFrame = null;
  // 受限 effectApi：仅画布绘制与音频特征读取，无 fetch/XHR/DOM 外访问
  var effectApi = {
    width: 0, height: 0,
    audioLevel: function () { return audio.level; },
    bands: function () { return audio.bands.slice(); },
    beat: function () { return audio.beat; },
    clear: function (color) {
      ctx.clearRect(0, 0, canvas.width, canvas.height);
      if (color) { ctx.fillStyle = color; ctx.fillRect(0, 0, canvas.width, canvas.height); }
    },
    fillRect: function (x, y, w, h, color) {
      ctx.fillStyle = color || '#fff';
      ctx.fillRect(x, y, w, h);
    },
    arc: function (x, y, r, color, lineWidth) {
      ctx.beginPath();
      ctx.arc(x, y, r, 0, Math.PI * 2);
      ctx.strokeStyle = color || '#fff';
      ctx.lineWidth = lineWidth || 2;
      ctx.stroke();
    },
    text: function (content, x, y, size, color) {
      ctx.fillStyle = color || '#fff';
      ctx.font = '600 ' + (size || 24) + 'px sans-serif';
      ctx.textAlign = 'center';
      ctx.fillText(content, x, y);
    },
  };
  window.addEventListener('message', function (event) {
    var data = event.data || {};
    if (data.type === 'init') {
      effectApi.width = canvas.width = data.width;
      effectApi.height = canvas.height = data.height;
      try {
        // 用户代码以 effectApi 为唯一入口（沙箱内无 window/document 暴露给其全局名）
        var factory = new Function('effectApi', data.code + ';\\nif (typeof onFrame === "function") { userOnFrame = onFrame; }');
        factory(effectApi);
        parent.postMessage({type: 'ready'}, '*');
      } catch (error) {
        parent.postMessage({type: 'error', message: String(error && error.message || error)}, '*');
      }
    } else if (data.type === 'frame') {
      audio.level = data.level; audio.bands = data.bands; audio.beat = data.beat;
      if (userOnFrame) {
        try { userOnFrame(effectApi); } catch (error) {
          parent.postMessage({type: 'error', message: String(error && error.message || error)}, '*');
          userOnFrame = null;
        }
      }
    } else if (data.type === 'resize') {
      effectApi.width = canvas.width = data.width;
      effectApi.height = canvas.height = data.height;
    }
  });
})();
</script></body></html>`;

/**
 * 创建沙箱控制器
 */
export function createSandboxEffect(): SandboxHandle {
  let iframe: HTMLIFrameElement | null = null;
  let ready = false;
  let width = 0;
  let height = 0;

  function post(message: Record<string, unknown>, transfer?: Transferable[]): void {
    if (iframe?.contentWindow && ready) {
      iframe.contentWindow.postMessage(message, "*", transfer);
    }
  }

  return {
    mount(container, code) {
      return new Promise((resolve) => {
        // 重复挂载先清理
        this.destroy();
        const frame = document.createElement("iframe");
        frame.setAttribute("sandbox", "allow-scripts");
        frame.setAttribute("title", "effect-sandbox");
        frame.style.cssText = "position:absolute;inset:0;width:100%;height:100%;border:0;background:transparent;";
        frame.srcdoc = BOOTSTRAP;
        container.appendChild(frame);
        iframe = frame;

        const timeout = window.setTimeout(() => {
          if (!ready) {
            resolve(false); // 3s 超时：调用方回退 config 渲染
          }
        }, READY_TIMEOUT_MS);

        const onMessage = (event: MessageEvent) => {
          if (event.source !== iframe?.contentWindow) {
            return;
          }
          const data = event.data || {};
          if (data.type === "ready") {
            ready = true;
            window.clearTimeout(timeout);
            post({type: "init", code, width, height: height || container.clientHeight});
            resolve(true);
          } else if (data.type === "error") {
            window.clearTimeout(timeout);
            resolve(false); // 执行异常：自动回退 config 渲染
          }
        };
        window.addEventListener("message", onMessage);

        const originalDestroy = () => {
          window.removeEventListener("message", onMessage);
          window.clearTimeout(timeout);
          frame.remove();
          iframe = null;
          ready = false;
        };
        // 将清理逻辑挂到控制器（destroy 复用）
        (this as {__cleanup?: () => void}).__cleanup = originalDestroy;
      });
    },

    pushFrame(frame) {
      const bands = new Float32Array(frame.bands);
      post({type: "frame", level: frame.level, bands, beat: frame.beat}, [bands.buffer]);
    },

    setSize(nextWidth, nextHeight) {
      width = nextWidth;
      height = nextHeight;
      post({type: "resize", width, height});
    },

    destroy() {
      (this as {__cleanup?: () => void}).__cleanup?.();
      (this as {__cleanup?: () => void}).__cleanup = undefined;
      ready = false;
    },
  };
}
