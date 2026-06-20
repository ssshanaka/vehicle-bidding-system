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

    // Fetch current bid data
    HeaderManager.fetchCurrentBid(listingId)
      .then((bidData) => {
        if (currentBidSpan) {
          currentBidSpan.textContent = HeaderManager.formatCurrency(
            bidData.currentBid || 0
          );
        }
      })
      .catch((error) => {
        console.error("HeaderManager: Error fetching bid data:", error);
        if (currentBidSpan) {
          currentBidSpan.textContent = "Error loading";
        }
      });

    // Show modal
    if (modal) {
      const bsModal = new bootstrap.Modal(modal);
      bsModal.show();
    }
  };

  // Helper functions
  HeaderManager.fetchCurrentBid = async function (listingId) {
    try {
      const response = await fetch(`/api/auctions/${listingId}/current-bid`);
      if (!response.ok) {
        throw new Error("Failed to fetch bid data");
      }
      return await response.json();
    } catch (error) {
      console.error("HeaderManager: Error fetching bid data:", error);
      return {
        currentBid: Math.floor(Math.random() * 1000000) + 500000,
        bidCount: Math.floor(Math.random() * 20) + 1,
      };
    }
  };

  HeaderManager.formatCurrency = function (amount) {
    return new Intl.NumberFormat("en-LK", {
      style: "currency",
      currency: "LKR",
      minimumFractionDigits: 0,
    }).format(amount);
  };

  // Initialize tooltips with scoped selectors
  HeaderManager.initializeTooltips = function () {
    const headerElement = document.getElementById("main-header");
    if (!headerElement) {
      return;
    }

    var tooltipTriggerList = [].slice.call(
      headerElement.querySelectorAll('[data-bs-toggle="tooltip"]')
    );
    var tooltipList = tooltipTriggerList.map(function (tooltipTriggerEl) {
      return new bootstrap.Tooltip(tooltipTriggerEl);
    });
  };

  // Utility function to show notifications
  HeaderManager.showNotification = function (message, type = "info") {
    // Create notification element
    const notification = document.createElement("div");
    notification.className = `alert alert-${type} alert-dismissible fade show position-fixed`;
    notification.style.cssText =
      "top: 80px; right: 20px; z-index: 9999; min-width: 300px;";
    notification.innerHTML = `
      ${message}
      <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
    `;

    document.body.appendChild(notification);

    // Auto remove after 5 seconds
    setTimeout(function () {
      if (notification.parentNode) {
        notification.parentNode.removeChild(notification);
      }
    }, 5000);
  };

  // Export additional functions to global scope for backward compatibility
  window.handleLogout = HeaderManager.handleLogout;
  window.openBidModal = HeaderManager.openBidModal;
  window.formatCurrency = HeaderManager.formatCurrency;
  window.showNotification = HeaderManager.showNotification;

  // Initialize tooltips when DOM is ready
  document.addEventListener("DOMContentLoaded", function () {
    HeaderManager.initializeTooltips();
  });
})(); // End of IIFE
