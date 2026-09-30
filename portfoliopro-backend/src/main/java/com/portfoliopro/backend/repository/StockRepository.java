package com.portfoliopro.backend.repository;

import com.portfoliopro.backend.entity.Stock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StockRepository extends JpaRepository<Stock, Long> {
    Optional<Stock> findByTicker(String ticker);

    @Query("select s from Stock s where " +
            "(:query is null or lower(s.ticker) like lower(concat('%', :query, '%')) " +
            "or lower(s.companyName) like lower(concat('%', :query, '%'))) and " +
            "(:sector is null or lower(s.sector) = lower(:sector)) order by s.ticker")
    List<Stock> search(@Param("query") String query, @Param("sector") String sector);

    @Query("select distinct s.sector from Stock s where s.sector is not null order by s.sector")
    List<String> findDistinctSectors();
}
