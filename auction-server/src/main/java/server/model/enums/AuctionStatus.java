package server.model.enums;

// trạng thái của 1 phiên đấu giá
public enum AuctionStatus {
  OPEN, // khởi tạo phiên đấu giá nhưng chưa đặt giá được
  RUNNING,
  FINISHED, // kết thúc đấu giá, không thể đặt giá nữa
  PAID, // Hoàn tất: Người thắng đã thanh toán thành công
  CANCELED // Hủy bỏ: Không ai mua, người thắng bom hàng, hoặc admin hủy
}

// enum (Enumeration/ kiểu liệt kê) là một kiểu dữ liệu đặc biệt trong Java dùng
// để định nghĩa một tập hợp các hằng số cố định không bao giờ thay đổi. Khi
// dùng enum, ta đang áp dụng tính chất Type Safety. Nghĩa là ép buộc biến
// status chỉ được phép nhận những giá trị đã được hệ thống cấp phép từ trước.
// Không một ai có thể truyền vào một giá trị rác do gõ sai, gõ nhầm, gõ sai
// chính tả.
