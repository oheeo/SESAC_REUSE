package com.sesac.reuse.repository.member;

import com.sesac.reuse.entity.member.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member,Long> {

    public Optional<Member> findByEmail(String email);  // email을 이용하여 회원 정보 찾음


    public boolean existsByEmail(String email); //메서드명 s 주의

}