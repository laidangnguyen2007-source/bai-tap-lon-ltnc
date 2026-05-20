package server.dao;

import java.util.List;

import server.model.entity.WalletTransaction;
import server.model.enums.WalletTransactionType;
import server.model.enums.WalletReferenceType;

public interface WalletTransactionDao extends GenericDao<WalletTransaction> {

  List<WalletTransaction> findByUserId(Long userId);

  List<WalletTransaction> findByReference(Long referenceId, WalletReferenceType referenceType);

  List<WalletTransaction> findByType(WalletTransactionType type);
}