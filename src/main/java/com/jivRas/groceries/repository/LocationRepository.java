package com.jivRas.groceries.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.jivRas.groceries.entity.Location;

@Repository
public interface LocationRepository extends JpaRepository<Location, Long> {

    @Query("SELECT DISTINCT l.state FROM Location l ORDER BY l.state")
    List<String> findDistinctStates();

    @Query("SELECT DISTINCT l.city FROM Location l WHERE l.state = :state ORDER BY l.city")
    List<String> findCitiesByState(@Param("state") String state);

    @Query("SELECT DISTINCT l.pincode FROM Location l WHERE l.state = :state AND l.city = :city ORDER BY l.pincode")
    List<String> findPincodesByStateAndCity(@Param("state") String state, @Param("city") String city);
}
