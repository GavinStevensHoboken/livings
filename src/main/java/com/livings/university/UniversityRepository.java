package com.livings.university;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.livings.geo.UsState;

public interface UniversityRepository extends JpaRepository<University, Long> {

    @Query("""
            select u from University u
            where (:state is null or u.state = :state)
              and (:q is null or lower(u.name) like lower(concat('%', :q, '%'))
                              or lower(u.city) like lower(concat('%', :q, '%')))
            order by u.name
            """)
    List<University> search(@Param("state") UsState state, @Param("q") String q);
}
