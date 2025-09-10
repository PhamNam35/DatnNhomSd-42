package com.example.AsmGD1.service.GiamGia;

import com.example.AsmGD1.entity.NguoiDung;
import com.example.AsmGD1.entity.PhieuGiamGia;
import com.example.AsmGD1.entity.PhuongThucThanhToan;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class GuiMailService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${app.brand.name:ACV Store}")
    private String brandName;

    @Value("${spring.mail.username:no-reply@acvstore.local}")
    private String defaultFrom;

    @Value("${app.web.base-url:https://acvstore.site}")
    private String baseUrl;

    private static final Locale VI_VN = new Locale("vi", "VN");
    private static final NumberFormat CURRENCY = NumberFormat.getCurrencyInstance(VI_VN);
    private static final NumberFormat NUMBER = NumberFormat.getNumberInstance(VI_VN);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // màu sắc theo mẫu 2
    private static final String BLUE       = "#2563eb";
    private static final String BLUE_DARK  = "#1e40af";
    private static final String GRAY_BODY  = "#f8fafc";
    private static final String CARD_BG    = "#f1f5f9";
    private static final String CARD_BD    = "#e2e8f0";
    private static final String TEXT_MAIN  = "#334155";
    private static final String TEXT_MUTE  = "#64748b";

    @Deprecated
    public void guiPhieuGiamGia(String toEmail, String tenPhieu, BigDecimal giaTri) {
        sendSimpleFallback(toEmail, tenPhieu, giaTri);
    }

    // ==== API chính (đã bỏ "Kiểu phiếu" + bỏ chữ brand cạnh logo) ====
    public void guiPhieuGiamGia(NguoiDung nguoiDung, PhieuGiamGia voucher) {
        if (nguoiDung == null || voucher == null || nguoiDung.getEmail() == null) return;

        final String subject = "Bạn nhận được phiếu giảm giá từ " + brandName;
        final String tenKh   = isBlank(nguoiDung.getHoTen()) ? "bạn" : escape(nguoiDung.getHoTen());

        final String tenPhieu = escape(nz(voucher.getTen()));
        final String maPhieu  = escape(nz(voucher.getMa()));
        final String giaTri   = formatGiaTri(voucher);                  // chỉ giá trị giảm

        final String soLuot   = voucher.getGioiHanSuDung()!=null ? NUMBER.format(voucher.getGioiHanSuDung()) : "—";
        final String pttt     = (voucher.getPhuongThucThanhToans()==null || voucher.getPhuongThucThanhToans().isEmpty())
                ? "Tất cả"
                : voucher.getPhuongThucThanhToans().stream()
                .filter(Objects::nonNull)
                .map(PhuongThucThanhToan::getTenPhuongThuc)
                .filter(s -> s!=null && !s.isBlank())
                .map(this::escape)
                .collect(Collectors.joining(", "));

        final String start = voucher.getNgayBatDau()!=null ? DATE_FMT.format(voucher.getNgayBatDau()) : "—";
        final String end   = voucher.getNgayKetThuc()!=null ? DATE_FMT.format(voucher.getNgayKetThuc()) : "—";
        final String cta   = baseUrl.replaceAll("/+$","") + "/voucher?code=" + urlEncode(maPhieu);

        String html = """
        <html>
        <head>
          <meta charset="UTF-8">
          <style>
            body{font-family:'Inter',-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,sans-serif;background:%s;margin:0;padding:20px;color:%s;line-height:1.55}
            .container{max-width:640px;margin:0 auto;background:#fff;border-radius:12px;box-shadow:0 4px 20px rgba(0,0,0,.05);padding:28px}
            h1{color:%s;text-align:left;font-size:26px;font-weight:700;margin:0 0 6px}
            .lead{font-size:15px;color:%s;margin:0 0 18px}
            .card{background:%s;border:1px solid %s;border-radius:10px;padding:18px}
            .row{margin:8px 0;font-size:15px;color:%s}
            .row b{color:#0f172a}
            .badge{display:inline-block;padding:6px 12px;border-radius:999px;background:#e8f0ff;color:%s;font-weight:800}
            .cta{margin:22px 0;text-align:center}
            .cta a{display:inline-block;padding:12px 24px;background:%s;color:#fff;text-decoration:none;border-radius:8px;font-weight:600}
            .cta a:hover{background:%s}
            .footer{margin-top:24px;padding-top:16px;border-top:1px solid %s;text-align:center;color:%s;font-size:12.5px}
          </style>
        </head>
        <body>
          <div class="container">
            <div style="display:flex;align-items:center;gap:10px;margin-bottom:10px">
              <div style="width:38px;height:38px;border-radius:10px;background:#eaf2ff;display:flex;align-items:center;justify-content:center;font-weight:800;color:%s">ACV</div>
            </div>
            <h1>Chúc mừng %s!</h1>
            <p class="lead">Bạn vừa nhận được một <b>phiếu giảm giá</b> từ <b>%s</b>. Chi tiết như dưới đây:</p>

            <div class="card">
              <div class="row"><b>Tên phiếu:</b> %s</div>
              <div class="row"><b>Mã phiếu:</b> <span style="color:%s;font-weight:800;letter-spacing:.3px">%s</span></div>
              <div class="row"><b>Giá trị giảm:</b> <span class="badge">%s</span></div>
              <div class="row"><b>Hiệu lực:</b></div>
              <div class="row" style="margin-left:12px"><span style="color:%s">Từ:</span> <b>%s</b></div>
              <div class="row" style="margin-left:12px"><span style="color:%s">Đến:</span> <b>%s</b></div>
              <div class="row"><b>Số lượt sử dụng:</b> %s</div>
              <div class="row"><b>Phương thức thanh toán áp dụng:</b> %s</div>
            </div>

            <div class="cta">
              <a href="%s">Sử dụng ngay tại %s</a>
            </div>

            <div class="footer">
              Email này được gửi tự động bởi %s. Vui lòng không trả lời.
            </div>
          </div>
        </body>
        </html>
        """.formatted(
                GRAY_BODY, TEXT_MAIN,
                BLUE, TEXT_MAIN,
                CARD_BG, CARD_BD, TEXT_MAIN, BLUE,
                BLUE, BLUE_DARK, CARD_BD, TEXT_MUTE,

                BLUE,                               // màu chữ trong ô logo "ACV"

                escape(tenKh), escape(brandName),

                tenPhieu,
                BLUE, maPhieu,
                giaTri,
                TEXT_MUTE, escape(start),
                TEXT_MUTE, escape(end),
                soLuot,
                pttt,

                cta, escape(brandName),
                escape(brandName)
        );

        try {
            MimeMessage mime = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mime, "UTF-8");
            helper.setFrom(defaultFrom, brandName);
            helper.setTo(nguoiDung.getEmail());
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(mime);
        } catch (Exception ex) {
            sendSimpleFallback(nguoiDung.getEmail(), voucher.getTen(), voucher.getGiaTriGiam());
        }
    }

    /* ================= helpers ================= */

    private String formatGiaTri(PhieuGiamGia v) {
        if (v.getLoai() == null || v.getGiaTriGiam() == null) return "—";
        if ("PERCENT".equalsIgnoreCase(v.getLoai())) {
            return v.getGiaTriGiam().stripTrailingZeros().toPlainString().replace(",", ".") + "%";
        }
        return CURRENCY.format(v.getGiaTriGiam()); // CASH
    }

    private String nz(String s){ return s==null? "": s.trim(); }
    private boolean isBlank(String s){ return s==null || s.trim().isEmpty(); }
    private String escape(String s){
        if (s==null) return "";
        return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;");
    }
    private String urlEncode(String s){
        try { return java.net.URLEncoder.encode(s, java.nio.charset.StandardCharsets.UTF_8); }
        catch (Exception e){ return s; }
    }

    private void sendSimpleFallback(String toEmail, String tenPhieu, BigDecimal giaTri){
        try{
            MimeMessage mime = mailSender.createMimeMessage();
            MimeMessageHelper h = new MimeMessageHelper(mime, "UTF-8");
            h.setFrom(defaultFrom, brandName);
            h.setTo(toEmail);
            h.setSubject("Bạn nhận được phiếu giảm giá từ " + brandName + "!");
            String body = """
              <div style="font-family:Inter,Segoe UI,Arial,sans-serif;line-height:1.6;color:#111">
                <h2 style="margin:0 0 8px">Chúc mừng!</h2>
                <p>Bạn vừa nhận được phiếu giảm giá: <b>%s</b></p>
                <p>Giá trị giảm: <b>%s</b></p>
              </div>
            """.formatted(escape(tenPhieu), giaTri!=null? CURRENCY.format(giaTri): "—");
            h.setText(body, true);
            mailSender.send(mime);
        }catch(Exception ignore){}
    }
}
