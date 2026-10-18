package com.example.sonata_brain.receipts;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ReceiptTemplateRepository extends JpaRepository<ReceiptTemplate, Long> {

    /** REQ-090.AC1: the built-in template first, then by name ignoring case. */
    @Query("select t from ReceiptTemplate t order by t.builtIn desc, lower(t.name), t.id")
    List<ReceiptTemplate> findAllInOrder();

    @Query("select t from ReceiptTemplate t where t.isDefault = true")
    Optional<ReceiptTemplate> findDefault();

    @Query("select count(t) > 0 from ReceiptTemplate t where lower(t.name) = lower(:name) and (:id is null or t.id <> :id)")
    boolean nameTaken(String name, Long id);

    /** Clears the default flag everywhere except on {@code id}, so the unique index allows the new default. */
    @Modifying(flushAutomatically = true)
    @Query("update ReceiptTemplate t set t.isDefault = false where t.isDefault = true and t.id <> :id")
    void clearDefaultExcept(Long id);

    @Modifying(flushAutomatically = true)
    @Query("update ReceiptTemplate t set t.isDefault = true where t.builtIn = true")
    void makeBuiltInDefault();
}
