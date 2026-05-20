// Interface chung cho tất cả DAO, định nghĩa các thao tác CRUD cơ bản
// Sử dụng Generic <T> để các DAO con có thể tái sử dụng mà không cần viết lại
// T phải kế thừa BaseEntity để đảm bảo có id và createdAt
package server.dao;

import java.util.List;
import java.util.Optional;
import server.model.entity.BaseEntity;

public interface GenericDao<T extends BaseEntity> {

  // Lưu entity mới vào hệ thống. Nếu entity chưa có ID, hệ thống sẽ tự gán
  T save(T entity);

  // Tìm entity theo ID. Trả về hộp Optional để tránh NullPointerException
  Optional<T> findById(Long id);

  // Quét sạch bộ nhớ, trả về 1 List chứa toàn bộ danh sách
  List<T> findAll();

  // Ghi đè lên entity đã tồn tại. Entity phải có ID hợp lệ
  T update(T entity);

  // Xóa entity theo ID. Trả về true nếu xóa thành công
  boolean deleteById(Long id);

  // Kiểm tra entity có tồn tại theo ID hay không
  boolean existsById(Long id);

  // Đếm tổng số entity trong hệ thống
  long count();
}
