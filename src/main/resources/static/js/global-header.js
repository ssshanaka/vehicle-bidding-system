/* Global Header Initialization - Lanka Auto Traders */
/* Scoped to prevent conflicts with page-level JavaScript */

(function () {
  "use strict";

  // Header-specific namespace to prevent global conflicts
  window.HeaderManager = {
    initialized: false,
    dropdowns: [],
    modals: [],
  };

  // Initialize header functionality on every page load
  document.addEventListener("DOMContentLoaded", function () {
    // Wait a bit to ensure all elements are rendered and other scripts have loaded
    setTimeout(function () {
      HeaderManager.initialize();
    }, 200);
  });

  // Main initialization function
  HeaderManager.initialize = function () {
    if (HeaderManager.initialized) {
      return; // Prevent double initialization
    }

    console.log("HeaderManager: Initializing header functionality...");

    HeaderManager.initializeHeaderDropdowns();
    HeaderManager.initializeHeaderModals();
    HeaderManager.initializeHeaderNavbar();

    HeaderManager.initialized = true;
    console.log("HeaderManager: Header functionality initialized successfully");
  };

  // Initialize header dropdowns with scoped selectors
  HeaderManager.initializeHeaderDropdowns = function () {
    if (typeof bootstrap === "undefined") {
      console.warn(
        "HeaderManager: Bootstrap not loaded, dropdowns may not work"
      );
      return;
    }

    // Only target dropdowns within the header
    const headerElement = document.getElementById("main-header");
    if (!headerElement) {
      console.warn("HeaderManager: Header element not found");
      return;
    }

    const dropdownElementList =
      headerElement.querySelectorAll(".dropdown-toggle");
    if (dropdownElementList.length > 0) {
      HeaderManager.dropdowns = [...dropdownElementList].map(
        (dropdownToggleEl) => new bootstrap.Dropdown(dropdownToggleEl)
      );
      console.log(
        "HeaderManager: Header dropdowns initialized:",
        HeaderManager.dropdowns.length
      );
    }
  };

  // Initialize header modals with scoped selectors
  HeaderManager.initializeHeaderModals = function () {
    const headerElement = document.getElementById("main-header");
    if (!headerElement) {
      console.warn("HeaderManager: Header element not found");
      return;
    }

    // Login modal
    const loginModal = headerElement.querySelector("#loginModal");
    if (loginModal) {
      const loginForm = loginModal.querySelector("#loginForm");
      if (loginForm) {
        loginForm.addEventListener("submit", function (e) {
          e.preventDefault();
          console.log("HeaderManager: Login form submitted");
        });
      }
    }

    // Bid modal
    const bidModal = headerElement.querySelector("#bidModal");
    if (bidModal) {
      const bidForm = bidModal.querySelector("#bidForm");
      if (bidForm) {
        bidForm.addEventListener("submit", function (e) {
          e.preventDefault();
          console.log("HeaderManager: Bid form submitted");
        });
      }
    }
  };

  // Initialize header navbar with scoped selectors
  HeaderManager.initializeHeaderNavbar = function () {
    const headerElement = document.getElementById("main-header");
    if (!headerElement) {
      console.warn("HeaderManager: Header element not found");
      return;
    }

    const navbar = headerElement.querySelector(".navbar");
    const navbarToggler = headerElement.querySelector(".navbar-toggler");
    const navbarCollapse = headerElement.querySelector(".navbar-collapse");

    if (navbarToggler && navbarCollapse) {
      navbarToggler.addEventListener("click", function () {
        navbarCollapse.classList.toggle("show");
      });
    }

    // Close mobile menu when clicking outside
    document.addEventListener("click", function (event) {
      if (
        !navbar.contains(event.target) &&
        navbarCollapse.classList.contains("show")
      ) {
        navbarCollapse.classList.remove("show");
      }
    });
  };

  // Export functions for global use with scoped namespace
  HeaderManager.handleLogout = function () {
    if (confirm("Are you sure you want to logout?")) {
      // Clear JWT cookies on client side
      document.cookie = "JWT=; Path=/; Max-Age=0; HttpOnly;";
      document.cookie = "JWT=; Path=/; Max-Age=0;";
      document.cookie = "JWT=; Path=/; Expires=Thu, 01 Jan 1970 00:00:00 GMT;";
      document.cookie =
        "JWT=; Path=/; Expires=Thu, 01 Jan 1970 00:00:00 GMT; HttpOnly;";
      document.cookie =
        "JWT=; Domain=" + window.location.hostname + "; Path=/; Max-Age=0;";
      document.cookie =
        "JWT=; Domain=." + window.location.hostname + "; Path=/; Max-Age=0;";
      console.log("HeaderManager: JWT cookies cleared on client side");
      return true;
    }
    return false;
  };

  HeaderManager.openBidModal = function (listingId) {
    const headerElement = document.getElementById("main-header");
    if (!headerElement) {
      console.warn("HeaderManager: Header element not found");
      return;
    }

    const modal = headerElement.querySelector("#bidModal");
    const listingIdInput = headerElement.querySelector("#bidListingId");
    const currentBidSpan = headerElement.querySelector("#currentBidAmount");

    if (listingIdInput) {
      listingIdInput.value = listingId;
    }

    if (currentBidSpan) {
      currentBidSpan.textContent = "Loading...";
    }

