package com.sesac.reuse.controller;


import com.sesac.reuse.dto.member.MemberDTO;
import com.sesac.reuse.exception.EmailExistException;
import com.sesac.reuse.service.member.MemberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

@Controller
@RequiredArgsConstructor
@Log4j2
public class MemberController {

    private final MemberService memberService;

    // 시큐리티 default login페이지를 안쓰고 커스텀 쓰는경우에는 GET요청 Controller 생성해줘야함
    @GetMapping("/auth2/login")
    public String loginPage() {
        return "member/login";
    }

    //시큐리티 기본 제공 user로 로그인 테스트하면 "/login?error로 리다이렉트되고 SecurityContext 에 저장안됐다고 나오는데
    //익명 사용자(시큐리티 제공 user)라 세션(SecurityContext)에 저장 안하는 최적화임.
    //저장되는거 확인하고싶다면, 메모리 유저 or 테스트코드로


    @GetMapping("/auth2/logout")
    public String logout(HttpServletRequest request, HttpServletResponse response) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();  // 현재 인증 객체(Authentication)를 가져옴
        if (authentication != null) {  // 가져온 인증 객체를 이용하여 SecurityContextLogoutHandler를 통해 로그아웃 실행
            new SecurityContextLogoutHandler().logout(request, response, authentication);
        }

        return "redirect:/";
    }


    @GetMapping("/auth2/signup")
    public String signUpPage() {
        return "member/signup";
    }

    @PostMapping("/auth2/signup")
    public String signUp(@Valid MemberDTO memberDTO, BindingResult bindingResult,
                         RedirectAttributes redirectAttributes,
                         Model model) {

        log.info("memberDTO={}", memberDTO);

        //validation 부분이 안먹혀서 추가 검증 로직 필요
//        if(bindingResult.hasErrors()) {
//            log.error(bindingResult.getFieldError("email").toString());
//            model.addAttribute("memberDTO",memberDTO);
//            return "member/signup"; //타임리프는 앞에 /안붙이는게 적합
////            return "redirect:/member/signup";
//        }

        validatePwAndRedirect(memberDTO, bindingResult, "/auth2/signup");


        try {
            memberService.join(memberDTO);
        } catch (EmailExistException e) {
            log.error("이미 존재하는 회원입니다."); // 프론트단으로 에러보내주기
            redirectAttributes.addFlashAttribute("error", "email"); //리다이렉트 컨트롤러에 세션(임시)로 담아 넘김 -> @ModelAttribute로 접근, 프론트단은 Model객체로 접근
            //여기선 어차피 GET으로가니까 컨트롤러에서는 접근할 필요없고, 프론트단에서만 접근하겠지
            return "redirect:/auth2/signup";
        }

        redirectAttributes.addFlashAttribute("result", "success");
        return "redirect:/auth2/login";
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/auth2/profile")
    public String myProfile(Model model) {

        String principalEmail = getPrincipalEmail();

        MemberDTO profileDTO = memberService.findProfileByEmail(principalEmail);

        log.info("profileDTO={}", profileDTO);
        model.addAttribute("profileDTO", profileDTO);

        return "member/profile";
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/auth2/modify-profile")
    public String modifyProfie(@Valid MemberDTO memberDTO, BindingResult bindingResult, Model model) {
        //굳이 profie변경용 DTO를 안만들어도 될거같음! (했다 로직 변경 )
        log.info("memberDTO={}", memberDTO);

        if (bindingResult.hasErrors()) {
            model.addAttribute("bindingResult", bindingResult.getAllErrors());
            return "member/profile";
        }

        validatePwAndRedirect(memberDTO, bindingResult, "/member/profile");

        //이제 DB에 로직 변경을 해야함
        memberService.modifyProfile(memberDTO);

        return "redirect:/auth2/profile";
    }


    private static String validatePwAndRedirect(MemberDTO memberDTO, BindingResult bindingResult, String redirectUrl) {
        if (!memberDTO.getPw().equals(memberDTO.getConfirmPw())) {
            bindingResult.rejectValue("pw", "passwordInCorrect", "비밀번호와 확인 비밀번호가 불일치합니다.");
            log.error("occur passwordInCorrect");

            return "redirect:" + redirectUrl;
        }
        return null;
    }

    private static String getPrincipalEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        log.info("email={}", email);

        return email;
    }

    @GetMapping("/auth2/reset-pwd")
    public String resetPwd() {
        log.info("호출됨");
        return "/member/reset-pwd";
    }

}
