import com.auction.server.model.entity.BidTransaction;

public class TestBidTransaction {

  public static void main(String[] args) {
    // 1. Giả sử: Auction ID là 10, Người đặt giá ID là 99, Số tiền là 1200.5
    Long auctionId = 10L;
    Long bidderId = 99L;
    double amount = 1200.5;

    // 2. Tạo bản ghi đặt giá (Sử dụng Constructor 3 tham số)
    BidTransaction bid = new BidTransaction(auctionId, bidderId, amount);

    // 3. In ra kiểm tra
    System.out.println("--- Chi tiết lượt đặt giá ---");
    System.out.println("Phiên đấu giá số: " + bid.getAuctionId());
    System.out.println("ID người đặt: " + bid.getBidderId());
    System.out.println("Số tiền: " + bid.getAmount());
    System.out.println("Thời điểm ghi nhận: " + bid.getTimestamp());

    // 4. Test hàm toString (Cái này rất tiện để debug)
    System.out.println("Dữ liệu thô: " + bid.toString());

    // Thử uncomment dòng dưới xem máy có mắng không?
    // bid.setAmount(2000); // Sẽ lỗi vì không có hàm setter (tính bất biến)
  }
}
