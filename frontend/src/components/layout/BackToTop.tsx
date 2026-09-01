"use client";

import {AnimatePresence, motion} from "framer-motion";
import {useEffect, useState} from "react";

/**
 * 返回顶部按钮（UI-Plan §2.8 A8）：滚动超过 1 屏后 blur-fade 上浮出现，
 * hover 整卡反色（T2，.hover-card），点击平滑滚顶。
 */
export default function BackToTop() {
  const [visible, setVisible] = useState(false);

  useEffect(() => {
    const onScroll = () => setVisible(window.scrollY > window.innerHeight);
    onScroll();
    window.addEventListener("scroll", onScroll, {passive: true});
    return () => window.removeEventListener("scroll", onScroll);
  }, []);

  return (
    <AnimatePresence>
      {visible && (
        <motion.button
          type="button"
          aria-label="Back to top"
          onClick={() => window.scrollTo({top: 0, behavior: "smooth"})}
          className="hover-card fixed bottom-6 right-6 z-40 flex h-11 w-11 items-center justify-center
                     rounded-pill border border-border shadow-aether"
          initial={{opacity: 0, y: 16}}
          animate={{opacity: 1, y: 0}}
          exit={{opacity: 0, y: 16}}
          transition={{duration: 0.3, ease: [0.22, 1, 0.36, 1]}}
        >
          {/* 上箭头（衬线花饰风格） */}
          <span aria-hidden className="text-lg leading-none">↑</span>
        </motion.button>
      )}
    </AnimatePresence>
  );
}
