package com.sliit.vehiclebiddingsystem.controller;

import java.time.Year;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.sliit.vehiclebiddingsystem.entity.User;
import com.sliit.vehiclebiddingsystem.entity.VehicleImage;
import com.sliit.vehiclebiddingsystem.entity.VehicleListing;
import com.sliit.vehiclebiddingsystem.repository.UserRepository;
import com.sliit.vehiclebiddingsystem.repository.VehicleImageRepository;
import com.sliit.vehiclebiddingsystem.repository.VehicleListingRepository;
import com.sliit.vehiclebiddingsystem.service.FileUploadService;

import jakarta.validation.Valid;

@Controller
public class VehicleListingController {

	@Autowired
	private VehicleListingRepository listingRepository;
	
	@Autowired
	private UserRepository userRepository;
	
	@Autowired
	private FileUploadService fileUploadService;
	
	@Autowired
	private VehicleImageRepository vehicleImageRepository;

	// PUBLIC: browse listings
	@GetMapping("/listings")
	public String publicListings(
			@RequestParam(required = false) String search,
			@RequestParam(required = false) Integer yearFrom,
			@RequestParam(required = false) Integer yearTo,
			@RequestParam(required = false) String condition,
			@RequestParam(required = false) Double maxPrice,
			Model model) {
		
		List<VehicleListing> listings = listingRepository.findAll().stream()
			.filter(l -> l.getStatus() == VehicleListing.Status.APPROVED)
			.filter(l -> search == null || search.isBlank() ||
				l.getMake().toLowerCase().contains(search.toLowerCase()) ||
				l.getModel().toLowerCase().contains(search.toLowerCase()))
			.filter(l -> yearFrom == null || l.getYear() >= yearFrom)
			.filter(l -> yearTo == null || l.getYear() <= yearTo)
			.filter(l -> condition == null || condition.isBlank() || 
				l.getCondition().name().equalsIgnoreCase(condition))
			.filter(l -> {
				if (maxPrice == null) return true;
				// Check if there are any bids and if the highest bid is within maxPrice
				if (l.getBids() != null && !l.getBids().isEmpty()) {
					double highestBid = l.getBids().stream()
						.mapToDouble(bid -> bid.getAmount())
						.max()
						.orElse(0.0);
					return highestBid <= maxPrice;
				}
				return true; // No bids yet, so price filter doesn't apply
			})
			.toList();
		
		model.addAttribute("listings", listings);
		model.addAttribute("search", search == null ? "" : search);
		model.addAttribute("yearFrom", yearFrom);
		model.addAttribute("yearTo", yearTo);
		model.addAttribute("condition", condition);
		model.addAttribute("maxPrice", maxPrice);
		return "public-listings";
	}

	@GetMapping("/listings/{id}")
	public String publicListingDetails(@PathVariable Long id, Model model) {
		VehicleListing listing = listingRepository.findById(id).orElseThrow();
		model.addAttribute("listing", listing);
		return "public-listing-details";
	}

	// SELLER AREA
	@GetMapping("/seller/listings")
	@PreAuthorize("hasRole('USER') or hasRole('ADMIN_OFFICER') or hasRole('IT_CONSULTANT') or hasRole('SALES_MANAGER') or hasRole('CUSTOMER_SERVICE') or hasRole('VEHICLE_INSPECTOR')")
	public String myListings(Authentication auth, @RequestParam(required = false) String status, Model model) {
		User user = userRepository.findByUsername(auth.getName());
		List<VehicleListing> listings = listingRepository.findAll().stream()
			.filter(l -> l.getSeller() != null && l.getSeller().getUserId().equals(user.getUserId()))
			.filter(l -> status == null || status.isBlank() || l.getStatus().name().equalsIgnoreCase(status))
			.toList();
		model.addAttribute("listings", listings);
		model.addAttribute("selectedStatus", status == null ? "" : status);
		return "seller-listings";
	}

	@GetMapping("/seller/listings/create")
	@PreAuthorize("hasRole('USER') or hasRole('ADMIN_OFFICER') or hasRole('IT_CONSULTANT') or hasRole('SALES_MANAGER') or hasRole('CUSTOMER_SERVICE') or hasRole('VEHICLE_INSPECTOR')")
	public String createForm(Model model) {
		VehicleListing listing = new VehicleListing();
		listing.setImages(new ArrayList<>());
		model.addAttribute("listing", listing);
		model.addAttribute("maxYear", Year.now().getValue());
		return "seller-create-listing";
	}

	@PostMapping("/seller/listings/create")
	@PreAuthorize("hasRole('USER') or hasRole('ADMIN_OFFICER') or hasRole('IT_CONSULTANT') or hasRole('SALES_MANAGER') or hasRole('CUSTOMER_SERVICE') or hasRole('VEHICLE_INSPECTOR')")
	@Transactional
	public String createSubmit(Authentication auth, @Valid @ModelAttribute("listing") VehicleListing listing, BindingResult result, Model model,
			@RequestParam(name = "imageUrls", required = false) List<String> imageUrls,
			@RequestParam(name = "imageFiles", required = false) List<MultipartFile> imageFiles) {
		try {
			System.out.println("DEBUG: Creating listing for user: " + auth.getName());
			System.out.println("DEBUG: Form data received:");
			System.out.println("  Make: " + listing.getMake());
			System.out.println("  Model: " + listing.getModel());
			System.out.println("  Year: " + listing.getYear());
			System.out.println("  Mileage: " + listing.getMileage());
			System.out.println("  Condition: " + listing.getCondition());
			System.out.println("  FuelType: " + listing.getFuelType());
			System.out.println("  Transmission: " + listing.getTransmission());
			System.out.println("  Description: " + listing.getDescription());
			
			validateListing(listing, result);
			if (result.hasErrors()) {
				System.out.println("DEBUG: Validation errors found");
				result.getAllErrors().forEach(error -> System.out.println("  Error: " + error.getDefaultMessage()));
				model.addAttribute("maxYear", Year.now().getValue());
				return "seller-create-listing";
			}
			User user = userRepository.findByUsername(auth.getName());
			if (user == null) {
				System.out.println("DEBUG: User not found: " + auth.getName());
				model.addAttribute("error", "User not found. Please log in again.");
				return "seller-create-listing";
			}
			System.out.println("DEBUG: Found user: " + user.getUsername() + ", passwordHash: " + (user.getPasswordHash() != null ? "SET" : "NULL"));
			
			// Ensure user has all required fields
			if (user.getPasswordHash() == null) {
				System.out.println("DEBUG: User passwordHash is null, this should not happen");
				model.addAttribute("error", "User data is corrupted. Please contact administrator.");
				return "seller-create-listing";
			}
			
			// Use a managed user reference to avoid cascade issues
			User managedUser = userRepository.findById(user.getUserId()).orElse(user);
			listing.setSeller(managedUser);
			listing.setStatus(VehicleListing.Status.PENDING);
			
			// Save listing first to get the ID
			System.out.println("DEBUG: Saving listing to database...");
			VehicleListing savedListing = listingRepository.save(listing);
			System.out.println("DEBUG: Listing saved successfully with ID: " + savedListing.getListingId());
			
			List<VehicleImage> imgs = new ArrayList<>();
			
			// Handle uploaded files
			if (imageFiles != null && !imageFiles.isEmpty()) {
				try {
					// Filter out empty files
					List<MultipartFile> validFiles = imageFiles.stream()
						.filter(file -> file != null && !file.isEmpty())
						.collect(java.util.stream.Collectors.toList());
					
					if (!validFiles.isEmpty()) {
						MultipartFile[] fileArray = validFiles.toArray(new MultipartFile[0]);
						List<String> uploadedUrls = fileUploadService.uploadFiles(fileArray);
						for (String url : uploadedUrls) {
							VehicleImage vi = new VehicleImage();
							vi.setListing(savedListing); // Use savedListing instead of listing
							vi.setImageUrl(url);
							imgs.add(vi);
						}
					}
				} catch (Exception e) {
					result.rejectValue("images", "error.images", "Failed to upload images: " + e.getMessage());
					model.addAttribute("maxYear", Year.now().getValue());
					return "seller-create-listing";
				}
			}
			
			// Handle URL inputs (existing functionality)
			if (imageUrls != null) {
				for (String url : imageUrls) {
					if (url == null || url.isBlank()) continue;
					VehicleImage vi = new VehicleImage();
					vi.setListing(savedListing); // Use savedListing instead of listing
					vi.setImageUrl(url.trim());
					imgs.add(vi);
				}
			}
			
			// Fix 2: Validate URLs before saving
			imgs.removeIf(img -> img.getImageUrl() == null || img.getImageUrl().isBlank());
			
			// Validate minimum image count
			if (imgs.size() < 3) {
				result.rejectValue("images", "error.images", "At least 3 images are required.");
				model.addAttribute("maxYear", Year.now().getValue());
				return "seller-create-listing";
			}
			
			// Save images manually using the repository
			for (VehicleImage img : imgs) {
				vehicleImageRepository.save(img);
			}
			System.out.println("DEBUG: Images saved successfully");
		return "redirect:/seller/listings";
		} catch (Exception e) {
			System.out.println("DEBUG: Error creating listing: " + e.getMessage());
			e.printStackTrace();
			model.addAttribute("error", "Failed to create listing: " + e.getMessage());
			model.addAttribute("maxYear", Year.now().getValue());
			return "seller-create-listing";
		}
	}

	@GetMapping("/seller/listings/edit/{id}")
	@PreAuthorize("hasRole('USER') or hasRole('ADMIN_OFFICER') or hasRole('IT_CONSULTANT') or hasRole('SALES_MANAGER') or hasRole('CUSTOMER_SERVICE') or hasRole('VEHICLE_INSPECTOR')")
	public String editForm(Authentication auth, @PathVariable Long id, Model model) {
		try {
			System.out.println("DEBUG: editForm called with ID: " + id);
			VehicleListing listing = listingRepository.findById(id).orElseThrow();
			System.out.println("DEBUG: Found listing with ID: " + listing.getListingId());
			User user = userRepository.findByUsername(auth.getName());
			if (!listing.getSeller().getUserId().equals(user.getUserId())) {
				model.addAttribute("error", "You can only edit your own listings.");
				return "redirect:/seller/listings";
			}
			
			// Check if listing can be edited (only PENDING or REJECTED)
			if (listing.getStatus() != VehicleListing.Status.PENDING && listing.getStatus() != VehicleListing.Status.REJECTED) {
				model.addAttribute("error", "Only pending or rejected listings can be edited. Approved listings are read-only.");
				return "redirect:/seller/listings";
			}
			
			// Extract image URLs to avoid circular reference in Thymeleaf
			List<String> imageUrls = new ArrayList<>();
			if (listing.getImages() != null) {
				for (VehicleImage image : listing.getImages()) {
					imageUrls.add(image.getImageUrl());
				}
			}
			
			System.out.println("DEBUG: Adding listing to model with ID: " + listing.getListingId());
			model.addAttribute("listing", listing);
			model.addAttribute("imageUrls", imageUrls);
			model.addAttribute("maxYear", Year.now().getValue());
			return "seller-edit-listing";
		} catch (Exception e) {
			model.addAttribute("error", "Failed to load listing: " + e.getMessage());
			return "redirect:/seller/listings";
		}
	}

	@PostMapping("/seller/listings/edit/{id}")
	@PreAuthorize("hasRole('USER') or hasRole('ADMIN_OFFICER') or hasRole('IT_CONSULTANT') or hasRole('SALES_MANAGER') or hasRole('CUSTOMER_SERVICE') or hasRole('VEHICLE_INSPECTOR')")
	@Transactional
	public String editSubmit(Authentication auth, @PathVariable Long id, @Valid @ModelAttribute("listing") VehicleListing form, BindingResult result,
			Model model, @RequestParam(name = "imageUrls", required = false) List<String> imageUrls) {
		try {
			System.out.println("DEBUG: editSubmit called with ID: " + id);
			System.out.println("DEBUG: Form data:");
			System.out.println("  Form listingId: " + form.getListingId());
			System.out.println("  Form make: " + form.getMake());
			System.out.println("  Form model: " + form.getModel());
			System.out.println("  Form year: " + form.getYear());
			System.out.println("  Form mileage: " + form.getMileage());
			System.out.println("  Form description: " + form.getDescription());
			System.out.println("  Form condition: " + form.getCondition());
			System.out.println("  Form fuelType: " + form.getFuelType());
			System.out.println("  Form transmission: " + form.getTransmission());
			System.out.println("  Image URLs count: " + (imageUrls != null ? imageUrls.size() : "null"));
			System.out.println("  BindingResult has errors: " + result.hasErrors());
			if (result.hasErrors()) {
				result.getAllErrors().forEach(error -> System.out.println("  Error: " + error.getDefaultMessage()));
			}
			
			VehicleListing listing = listingRepository.findById(id).orElseThrow();
			System.out.println("DEBUG: Found existing listing:");
			System.out.println("  Existing make: " + listing.getMake());
			System.out.println("  Existing model: " + listing.getModel());
			System.out.println("  Existing year: " + listing.getYear());
			System.out.println("  Existing mileage: " + listing.getMileage());
			System.out.println("  Existing description: " + listing.getDescription());
			User user = userRepository.findByUsername(auth.getName());
			if (!listing.getSeller().getUserId().equals(user.getUserId())) {
				model.addAttribute("error", "You can only edit your own listings.");
				return "redirect:/seller/listings";
			}
			
			if (!(listing.getStatus() == VehicleListing.Status.PENDING || listing.getStatus() == VehicleListing.Status.REJECTED)) {
				model.addAttribute("error", "Only pending or rejected listings can be edited. Approved listings are read-only.");
				return "redirect:/seller/listings";
			}
			
			validateListing(form, result);
			if (result.hasErrors()) {
				model.addAttribute("maxYear", Year.now().getValue());
				return "seller-edit-listing";
			}
			
			// Update listing fields
			System.out.println("DEBUG: Updating listing with form data:");
			System.out.println("  Make: " + form.getMake() + " -> " + listing.getMake());
			System.out.println("  Model: " + form.getModel() + " -> " + listing.getModel());
			System.out.println("  Year: " + form.getYear() + " -> " + listing.getYear());
			System.out.println("  Mileage: " + form.getMileage() + " -> " + listing.getMileage());
			System.out.println("  Description: " + form.getDescription() + " -> " + listing.getDescription());
			
			listing.setMake(form.getMake());
			listing.setModel(form.getModel());
			listing.setYear(form.getYear());
			listing.setMileage(form.getMileage());
			listing.setCondition(form.getCondition());
			listing.setFuelType(form.getFuelType());
			listing.setTransmission(form.getTransmission());
			listing.setDescription(form.getDescription());
			
			// Handle image replacement - always process image URLs from form
			System.out.println("DEBUG: Image URLs received: " + (imageUrls != null ? imageUrls.size() : "null") + " URLs");
			if (imageUrls != null) {
				for (int i = 0; i < imageUrls.size(); i++) {
					System.out.println("  URL " + i + ": '" + imageUrls.get(i) + "'");
				}
			}
			
			// Always process image URLs from form (they include existing + new images)
			List<VehicleImage> imgs = new ArrayList<>();
			if (imageUrls != null) {
				for (String url : imageUrls) {
					if (url == null || url.isBlank()) continue;
					VehicleImage vi = new VehicleImage();
					vi.setListing(listing);
					vi.setImageUrl(url.trim());
					imgs.add(vi);
				}
			}
			
			System.out.println("DEBUG: Total valid image URLs: " + imgs.size());
			
			// Validate minimum image count
			if (imgs.size() < 3) {
				result.rejectValue("images", "error.images", "At least 3 images are required.");
				model.addAttribute("maxYear", Year.now().getValue());
				return "seller-edit-listing";
			}
			
			listing.setImages(imgs);
			System.out.println("DEBUG: Images set to " + imgs.size() + " images");
			
			// If listing was rejected, change status back to PENDING for resubmission
			if (listing.getStatus() == VehicleListing.Status.REJECTED) {
				listing.setStatus(VehicleListing.Status.PENDING);
				listing.setRejectionReason(null); // Clear rejection reason
				System.out.println("DEBUG: Rejected listing status changed to PENDING for resubmission");
			}
			
			System.out.println("DEBUG: Saving listing to database...");
			VehicleListing savedListing = listingRepository.save(listing);
			System.out.println("DEBUG: Listing saved successfully with ID: " + savedListing.getListingId());
			System.out.println("DEBUG: Saved listing data:");
			System.out.println("  Saved make: " + savedListing.getMake());
			System.out.println("  Saved model: " + savedListing.getModel());
			System.out.println("  Saved year: " + savedListing.getYear());
			System.out.println("  Saved mileage: " + savedListing.getMileage());
			System.out.println("  Saved description: " + savedListing.getDescription());
			System.out.println("  Saved images count: " + (savedListing.getImages() != null ? savedListing.getImages().size() : 0));
			
			// Force flush to ensure changes are persisted
			listingRepository.flush();
			System.out.println("DEBUG: Database changes flushed successfully");
			
			model.addAttribute("success", "Listing updated successfully!");
			return "redirect:/seller/listings";
		} catch (Exception e) {
			model.addAttribute("error", "Failed to update listing: " + e.getMessage());
			model.addAttribute("maxYear", Year.now().getValue());
			return "seller-edit-listing";
		}
	}

	@PostMapping("/seller/listings/delete/{id}")
	@PreAuthorize("hasRole('USER') or hasRole('ADMIN_OFFICER') or hasRole('IT_CONSULTANT') or hasRole('SALES_MANAGER') or hasRole('CUSTOMER_SERVICE') or hasRole('VEHICLE_INSPECTOR')")
	public String delete(Authentication auth, @PathVariable Long id, Model model) {
		try {
			VehicleListing listing = listingRepository.findById(id).orElseThrow();
			User user = userRepository.findByUsername(auth.getName());
			
			if (!listing.getSeller().getUserId().equals(user.getUserId())) {
				model.addAttribute("error", "You can only delete your own listings.");
				return "redirect:/seller/listings";
			}
			
			// Prevent deletion of approved listings
			if (listing.getStatus() == VehicleListing.Status.APPROVED) {
				model.addAttribute("error", "Approved listings cannot be deleted. They are read-only.");
				return "redirect:/seller/listings";
			}
			
			listingRepository.delete(listing);
			model.addAttribute("success", "Listing deleted successfully!");
			return "redirect:/seller/listings";
		} catch (Exception e) {
			model.addAttribute("error", "Failed to delete listing: " + e.getMessage());
			return "redirect:/seller/listings";
		}
	}

	@GetMapping("/seller/listings/{id}")
	@PreAuthorize("hasRole('USER') or hasRole('ADMIN_OFFICER') or hasRole('IT_CONSULTANT') or hasRole('SALES_MANAGER') or hasRole('CUSTOMER_SERVICE') or hasRole('VEHICLE_INSPECTOR')")
	public String details(Authentication auth, @PathVariable Long id, Model model) {
		try {
			System.out.println("DEBUG: Loading listing details for ID: " + id);
			VehicleListing listing = listingRepository.findById(id).orElseThrow();
			System.out.println("DEBUG: Found listing: " + listing.getMake() + " " + listing.getModel());
			System.out.println("DEBUG: Listing images count: " + (listing.getImages() != null ? listing.getImages().size() : 0));
			
			User user = userRepository.findByUsername(auth.getName());
			
			// Check if user is the owner of the listing (unless they're an admin)
			boolean isAdmin = auth.getAuthorities().stream()
				.anyMatch(authority -> authority.getAuthority().startsWith("ROLE_ADMIN"));
			
			if (!isAdmin && !listing.getSeller().getUserId().equals(user.getUserId())) {
				System.out.println("DEBUG: Access denied - user is not owner and not admin");
				model.addAttribute("error", "You can only view your own listings.");
				return "redirect:/seller/listings";
			}
			
			System.out.println("DEBUG: Adding listing to model and returning template");
			model.addAttribute("listing", listing);
			return "seller-listing-details";
		} catch (Exception e) {
			System.out.println("DEBUG: Error loading listing details: " + e.getMessage());
			e.printStackTrace();
			model.addAttribute("error", "Failed to load listing details: " + e.getMessage());
			return "redirect:/seller/listings";
		}
	}

	private void validateListing(VehicleListing listing, BindingResult result) {
		if (listing.getYear() != null && listing.getYear() < 1990) {
			result.rejectValue("year", "year.min", "Year must be >= 1990");
		}
		if (listing.getYear() != null && listing.getYear() > Year.now().getValue()) {
			result.rejectValue("year", "year.max", "Year cannot exceed current year");
		}
		if (listing.getMileage() != null && listing.getMileage() <= 0) {
			result.rejectValue("mileage", "mileage.min", "Mileage must be positive");
		}
		if (listing.getFuelType() == null || listing.getFuelType().trim().isEmpty()) {
			result.rejectValue("fuelType", "fuelType.required", "Fuel type is required");
		}
		if (listing.getTransmission() == null || listing.getTransmission().trim().isEmpty()) {
			result.rejectValue("transmission", "transmission.required", "Transmission is required");
		}
		if (listing.getDescription() != null && listing.getDescription().length() > 1000) {
			result.rejectValue("description", "description.max", "Description cannot exceed 1000 characters");
		}
	}
}
