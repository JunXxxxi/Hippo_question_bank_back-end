package com.exam.dao;

import com.exam.entity.Exroom;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ExroomDAO extends JpaRepository<Exroom,Integer> {
    Exroom findByKid(int kid);
    @Query(nativeQuery =true,value = "SELECT * FROM exroom WHERE create_by = ?1")
    Page<Exroom> findAllByCreateBy(String username,Pageable pageable);
    List<Exroom> findAllByStarttimeBetween(long now,long ddl);
    @Query(nativeQuery =true,value = "select count(*) from exroom where create_time >= ?1-24*60*60*1000 and create_time < ?1 and create_by = ?2")
    Integer getNumExamPerDay(Long t, String name);
    @Query(nativeQuery =true,value = "select count(*) from exroom where create_time >= ?1-24*60*60*1000*30 and create_time < ?1 and create_by = ?2")
    Integer getNumExamPerMonth(Long t, String name);
    @Query(nativeQuery =true,value = "select count(*) from exroom where create_time >= ?1-24*60*60*1000*30 and create_time < ?1 and starttime > ?2 and create_by = ?3")
    Integer getNumNotStart(Long t, Long now, String name);
    @Query(nativeQuery =true,value = "select * from exroom where create_by = ?1 order by create_time desc limit 3")
    List<Exroom> getLastThree(String name);
    @Query(nativeQuery =true,value = "select * from exroom order by create_time desc limit 3")
    List<Exroom> getSLastThree();
    List<Exroom> findByStarttimeBefore(long now);
    List<Exroom> findByStarttimeAfter(long now);
    @Query(nativeQuery =true,value = "select pid from exroom where kid=?1")
    Integer findPidByKid(int kid);
    @Query(nativeQuery = true,value = "update exroom set nowStudentsNum = nowStudentsNum+1 where kid=?1")
    void updateStuNum(int kid);

}
