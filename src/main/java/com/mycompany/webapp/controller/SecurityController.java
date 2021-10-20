package com.mycompany.webapp.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import lombok.extern.slf4j.Slf4j;

@Controller
@Slf4j
@RequestMapping("/security")
public class SecurityController {

	@RequestMapping("/content")
	public String content(Authentication authentication) {
		log.info("실행");
		
		if(authentication != null && authentication.isAuthenticated()) {			
			log.info(authentication.getPrincipal().toString());
		}
		
		return "security/content";
	}

	@RequestMapping("/loginForm")
	public String loginForm() {
		log.info("실행");
		return "security/loginForm";
	}

	@RequestMapping("/loginError")
	public String loginError(Model model) {
		log.info("실행");
		model.addAttribute("loginError", true);
		return "security/loginForm";
	}

	@RequestMapping("/accessDenied")
	public String accessDenied() {
		log.info("실행");
		return "security/accessDenied";
	}

	@RequestMapping("/admin/action")
	public String adminAction() {
		log.info("실행");
		return "redirect:/security/content";
	}

	@RequestMapping("/manager/action")
	public String managerAction() {
		log.info("실행");
		return "redirect:/security/content";
	}

	@RequestMapping("/user/action")
	public String userAction() {
		log.info("실행");
		return "redirect:/security/content";
	}
}
