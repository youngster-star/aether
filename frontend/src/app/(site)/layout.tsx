import BackToTop from "@/components/layout/BackToTop";
import Footer from "@/components/layout/Footer";
import Navbar from "@/components/layout/Navbar";

/**
 * 用户端布局（UI-Plan §4.1）：Nav + 页面主体 + Footer + 返回顶部（A8）
 */
export default function SiteLayout({children}: {children: React.ReactNode}) {
  return (
    <>
      <Navbar />
      <main className="flex-1">{children}</main>
      <Footer />
      <BackToTop />
    </>
  );
}
