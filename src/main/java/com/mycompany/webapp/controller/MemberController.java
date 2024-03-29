package com.mycompany.webapp.controller;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.mycompany.webapp.dto.Event;
import com.mycompany.webapp.dto.MyCoupon;
import com.mycompany.webapp.dto.Pager;
import com.mycompany.webapp.dto.Product;
import com.mycompany.webapp.dto.ProductColor;
import com.mycompany.webapp.dto.ProductOrder;
import com.mycompany.webapp.dto.ShoppingBag;
import com.mycompany.webapp.security.CustomUserDetails;
import com.mycompany.webapp.service.EventService;
import com.mycompany.webapp.service.MyOrderService;
import com.mycompany.webapp.service.ProductService;
import com.mycompany.webapp.service.ShoppingbagService;

import lombok.extern.slf4j.Slf4j;

@Controller
@Slf4j
@RequestMapping("/member")
public class MemberController {

	@Resource
	ShoppingbagService shoppingbagService;
	@Resource
	ProductService productService;
	@Resource
	EventService eventService;
	@Resource
	MyOrderService myOrderService;

	@RequestMapping("/myorders")
	public String myOrdersList(HttpSession session, Model model, @RequestParam(defaultValue = "1") int pageNo,
			@RequestParam(defaultValue = "pcode") String type, @RequestParam(defaultValue = "") String keyword,
			HttpServletRequest httpServletRequest, Authentication authentication) { // 회원의 주문한 상품 리스트

		// 컨트롤러에서 로그인 정보 얻는 방법
		int mno = -1;
		if (authentication != null && authentication.isAuthenticated()) {
			CustomUserDetails customUserDetails = (CustomUserDetails) authentication.getPrincipal();
			mno = customUserDetails.getMno(); // 어떤 회원 인지
		}

		// 페이징 처리
		int totalRows = myOrderService.getProductOrderCount(mno, type, keyword);

		session.setAttribute("totalRows", totalRows);

		Pager pager = new Pager(5, 5, totalRows, pageNo);
		model.addAttribute("pager", pager);

		// 주문 테이블에서 mno에 해당하는 모든 정보를 가지고 옴
		List<ProductOrder> productOrders = myOrderService.getProductOrder(mno, pager, type, keyword);

		for (ProductOrder po : productOrders) {
			Product p = productService.getProduct(po.getPcode());
			po.setBname(p.getBname());
			po.setPname(p.getPname());
			po.setPprice(p.getPprice());

			// 상품이 가진 컬러 리스트를 가져온다.
			List<ProductColor> colors = productService.getProductColor(p);

			for (int i = 0; i < colors.size(); i++) {
				if (po.getPcolor().equals(colors.get(i).getCproductcolor())) {
					po.setCimageproduct1(colors.get(i).getCimageproduct1());
				}
			}
			po.setType(type);
			po.setKeyword(keyword);
		}

		model.addAttribute("type", type);
		model.addAttribute("keyword", keyword);
		model.addAttribute("productOrders", productOrders);

		return "member/myorders";
	}

	@RequestMapping("/mycoupons")
	public String myCoupons(HttpSession session, Model model, Authentication authentication) {

		// 내가 참여 하고 있는 이벤트들을 찾기 위해 세션에서 mno를 꺼내온다.
		CustomUserDetails customUserDetails = (CustomUserDetails) authentication.getPrincipal();
		int mno = customUserDetails.getMno();

		// mno로 내가 참여하고 있는 이벤트를 검색한다.
		List<MyCoupon> hadCoupons = eventService.getjoinedEvents(mno);
		List<Event> events = new ArrayList<Event>();
		for (int i = 0; i < hadCoupons.size(); i++) {
			// eno, eno로 쿠폰 만료기한과 쿠폰상태 확인
			MyCoupon temp = eventService.getCouponinfo(hadCoupons.get(i).getEno(), hadCoupons.get(i).getCno());
			hadCoupons.get(i).setCdate(temp.getCdate());
			hadCoupons.get(i).setCstate(temp.getCstate());

			// 이벤트 정보 추가
			events.add(eventService.getEventByEno(hadCoupons.get(i).getEno()));
		}

		model.addAttribute("hadCoupons", hadCoupons);
		model.addAttribute("events", events);

		return "member/mycoupons";
	}

	@RequestMapping("/shoppingbag")
	public String shoppingBag(HttpSession session, Model model, Authentication authentication) {

//		int mno = Integer.parseInt(session.getAttribute("mno").toString());
		// 컨트롤러에서 로그인 정보 얻는 방법
		int mno = -1;
		if (authentication != null && authentication.isAuthenticated()) {
			CustomUserDetails customUserDetails = (CustomUserDetails) authentication.getPrincipal();
			mno = customUserDetails.getMno();// 어떤 회원 인지
		}

		// mno라는 사람의 쇼핑백 정보를 DB 쇼핑백 테이블에서 가져온다.
		List<ShoppingBag> shoppingBags = shoppingbagService.getShoppingProducts(mno);

		for (ShoppingBag sb : shoppingBags) {
			Product p = productService.getProduct(sb.getPcode());

			sb.setPname(p.getPname());
			sb.setBname(p.getBname());
			sb.setPprice(p.getPprice());

			// 상품이 가진 컬러 리스트를 가져온다.
			List<ProductColor> colors = productService.getProductColor(p);
			sb.setColors(colors);

			for (int i = 0; i < colors.size(); i++) {
				if (sb.getSbproductcolor().equals(colors.get(i).getCproductcolor())) {
					sb.setColoridx(i);
					break;
				}
			}

			sb.setSizes(productService.getProductSize(p));
		}

		model.addAttribute("shoppingBags", shoppingBags);

		return "member/shoppingbag";
	}

	@RequestMapping("/shoppingbagForDirectOrder")
	public String shoppingbagForDirectOrder(HttpSession session, Model model, Authentication authentication) {

		CustomUserDetails customUserDetails = (CustomUserDetails) authentication.getPrincipal();
		int mno = customUserDetails.getMno();

		// mno라는 사람의 쇼핑백 정보를 DB 쇼핑백 테이블에서 가져온다.
		List<ShoppingBag> shoppingBags = shoppingbagService.getShoppingProducts(mno);

		// 가장 높은 번호가 최근에 주문한 쇼핑백 번호일 것이다.
		int maxVal = 0;

		for (ShoppingBag sb : shoppingBags) {
			maxVal = Math.max(maxVal, sb.getSbno());
		}

		return "redirect:/member/insertorder?checkedItems=" + String.valueOf(maxVal) + ",&itemsLength=1";
	}

	@PostMapping("/changeProductOption")
	public String changeProductOption(int sbnoSelected, String pcodeSelected, String colorSelected, String sizeSelected,
			int amountSelected, HttpSession session, Authentication authentication) {

		CustomUserDetails customUserDetails = (CustomUserDetails) authentication.getPrincipal();
		int mno = customUserDetails.getMno();

		ShoppingBag sbObject = new ShoppingBag();
		sbObject.setSbno(sbnoSelected);
		sbObject.setSbproductcolor(colorSelected);
		sbObject.setSbproductsize(sizeSelected);
		sbObject.setPcode(pcodeSelected);
		sbObject.setMno(mno);

		// 변경하려는 값이 이미 존재하는지 확인한다.
		int sbno = shoppingbagService.selectSbno(sbObject);

		if (sbno == -1) {
			// 쇼핑백에 이미 같은 종류의 상품이 담겨있으면 값을 갱신한다.
			shoppingbagService.updateShoppingbag(sbObject);
		} else {
			// 이미 존재한다면 변경하려는 행을 삭제하고, 이전에 존재하는 행을 갱신한다.
			if (sbnoSelected != sbno) {
				shoppingbagService.deleteShoppingbag(sbnoSelected);
				sbObject.setSbno(sbno);
			}
			shoppingbagService.updateShoppingbag(sbObject);
		}

		String scode = pcodeSelected + "_" + colorSelected + "_" + sizeSelected;
		int remainStock = productService.getProductStock(scode).getSproductamount();

		sbObject.setSbproductamount(Math.min(amountSelected, remainStock));
		shoppingbagService.updateShoppingbag(sbObject);

		// 너무 많은 디비 접근이 있을 거 같아서 변경하는 곳 한 군데만 재고를 보여준다.
		session.setAttribute("remainStock", remainStock);
		session.setAttribute("remainSbno", sbnoSelected);

		return "redirect:/member/shoppingbag";
	}

	@RequestMapping("/deleteshoppingbag")
	public String deleteShoppingbag(int sbnoSelected) {
		shoppingbagService.deleteShoppingbag(sbnoSelected);

		return "redirect:/member/shoppingbag";
	}

	@RequestMapping("/deleteallshoppingbag")
	public String deleteAllShoppingbag(HttpSession session, Authentication authentication) {

		CustomUserDetails customUserDetails = (CustomUserDetails) authentication.getPrincipal();
		int mno = customUserDetails.getMno();

		List<ShoppingBag> shoppingBags = shoppingbagService.getShoppingProducts(mno);

		for (ShoppingBag sb : shoppingBags) {
			shoppingbagService.deleteShoppingbag(sb.getSbno());
		}

		return "redirect:/member/shoppingbag";
	}

	@RequestMapping("/insertorder")
	public String insertOrder(String checkedItems, int itemsLength, HttpSession session) {

		session.setAttribute("checkedItems", checkedItems);
		session.setAttribute("itemsLength", itemsLength);

		return "redirect:/order/ordering";
	}
}