/**
 * Homepage JavaScript functionality for Vehicle Bidding System
 * Handles search, bidding, countdown timers, and user interactions
 */

class HomepageManager {
  constructor() {
    this.init();
  }

  init() {
    this.setupEventListeners();
    this.initializeCountdownTimers();
    this.setupFormValidation();
    this.setupBidModal();
    this.setupSearchForm();
  }

  /**
   * Setup all event listeners
   */
  setupEventListeners() {
    // Search form submission
    const searchForm = document.getElementById("searchForm");
    if (searchForm) {
      searchForm.addEventListener("submit", this.handleSearchSubmit.bind(this));
    }

    // Bid form submission
    const bidForm = document.getElementById("bidForm");
    if (bidForm) {
      bidForm.addEventListener("submit", this.handleBidSubmit.bind(this));
    }

    // Clear search button
    const clearBtn = document.querySelector('button[onclick="clearSearch()"]');
    if (clearBtn) {
      clearBtn.addEventListener("click", this.clearSearch.bind(this));
    }

    // Setup smooth scrolling for search vehicle button
    this.setupSmoothScrolling();

    // Modal events
    this.setupModalEvents();

    // Accessibility - Keyboard navigation for cards
    const cards = document.querySelectorAll(".featured-card");
    cards.forEach((card) => {
      card.addEventListener("keydown", (e) => {
        if (e.key === "Enter") {
          const viewBtn = card.querySelector(".btn-primary");
          if (viewBtn) viewBtn.click();
        }
      });
    });
  }

  /**
   * Setup modal-related events
   */
  setupModalEvents() {
    // Login modal
    const loginModal = document.getElementById("loginModal");
    if (loginModal) {
      loginModal.addEventListener("show.bs.modal", () => {
        this.focusFirstInput(loginModal);
      });
    }

    // Bid modal
    const bidModal = document.getElementById("bidModal");
    if (bidModal) {
      bidModal.addEventListener("show.bs.modal", () => {
        this.focusFirstInput(bidModal);
      });
    }
  }

  /**
   * Focus first input in modal
   */
  focusFirstInput(modal) {
    setTimeout(() => {
      const firstInput = modal.querySelector(
        'input[type="text"], input[type="email"], input[type="number"]'
      );
      if (firstInput) {
        firstInput.focus();
      }
    }, 150);
  }

  /**
   * Setup search form functionality
   */
  setupSearchForm() {
    const yearFrom = document.getElementById("yearFrom");
    const yearTo = document.getElementById("yearTo");

    if (yearFrom && yearTo) {
      // Auto-populate year range
      const currentYear = new Date().getFullYear();
      this.populateYearOptions(yearFrom, 1990, currentYear);
      this.populateYearOptions(yearTo, 1990, currentYear);

      // Set default values
      yearFrom.value = "";
      yearTo.value = "";

      // Swap years if from > to on change
      yearFrom.addEventListener("change", () =>
        this.validateYearRange(yearFrom, yearTo)
      );
      yearTo.addEventListener("change", () =>
        this.validateYearRange(yearFrom, yearTo)
      );
    }
  }

  /**
   * Validate year range dynamically
   */
  validateYearRange(from, to) {
    if (from.value && to.value && parseInt(from.value) > parseInt(to.value)) {
      const temp = from.value;
      from.value = to.value;
      to.value = temp;
      this.showAlert(
        "Year range auto-corrected (From cannot exceed To)",
        "info"
      );
    }
  }

  /**
   * Populate year options
   */
  populateYearOptions(selectElement, startYear, endYear) {
    for (let year = endYear; year >= startYear; year--) {
      const option = document.createElement("option");
      option.value = year;
      option.textContent = year;
      selectElement.appendChild(option);
    }
  }

  /**
   * Handle search form submission
   */
  handleSearchSubmit(event) {
    const yearFrom = document.getElementById("yearFrom");
    const yearTo = document.getElementById("yearTo");

    // Validate year range
    if (
      yearFrom.value &&
      yearTo.value &&
      parseInt(yearFrom.value) > parseInt(yearTo.value)
    ) {
      event.preventDefault();
      this.showAlert('Year "From" cannot be greater than "To"', "warning");
      return false;
    }

    // Show loading state
    const submitBtn = event.target.querySelector('button[type="submit"]');
    if (submitBtn) {
      this.setButtonLoading(submitBtn, true);
    }
  }

  /**
   * Clear search form
   */
  clearSearch() {
    const form = document.getElementById("searchForm");
    if (form) {
      form.reset();
      if (window.history.replaceState) {
        window.history.replaceState(
          {},
          document.title,
          window.location.pathname
        );
      }
    }
  }

  /**
   * Setup bid modal functionality
   */
  setupBidModal() {
    window.openBidModal = (listingId) => {
      this.openBidModal(listingId);
    };
  }

  /**
   * Open bid modal with listing data
   */
  openBidModal(listingId) {
    const modal = document.getElementById("bidModal");
    const listingIdInput = document.getElementById("bidListingId");
    const currentBidSpan = document.getElementById("currentBidAmount");

    if (listingIdInput) {
      listingIdInput.value = listingId;
    }

    if (currentBidSpan) {
      currentBidSpan.textContent = "Loading...";
    }

    this.fetchCurrentBid(listingId)
      .then((bidData) => {
        if (currentBidSpan) {
          currentBidSpan.textContent = this.formatCurrency(
            bidData.currentBid || 0
          );
        }
      })
      .catch((error) => {
        console.error("Error fetching bid data:", error);
        if (currentBidSpan) {
          currentBidSpan.textContent = "Unavailable";
        }
      });

    const bsModal = new bootstrap.Modal(modal);
    bsModal.show();
  }

  /**
   * Fetch current bid (mock)
   */
  async fetchCurrentBid(listingId) {
    return new Promise((resolve) => {
      setTimeout(() => {
        resolve({ currentBid: Math.floor(Math.random() * 1000000) + 100000 });
      }, 500);
    });
  }

  /**
   * Handle bid submission
   */
  handleBidSubmit(event) {
    event.preventDefault();
    const form = event.target;
    const amountInput = document.getElementById("bidAmount");
    const submitBtn = form.querySelector('button[type="submit"]');

    if (!amountInput.checkValidity()) {
      form.reportValidity();
      return;
    }

    const amount = parseFloat(amountInput.value);
    this.setButtonLoading(submitBtn, true);

    this.submitBid(amount)
      .then((response) => {
        if (response.success) {
          this.showAlert("Bid placed successfully!", "success");
          this.closeBidModal();
          this.updateCountdowns();
        } else {
          this.showAlert("Failed to place bid. Please try again.", "danger");
        }
      })
      .catch((error) => {
        console.error("Bid error:", error);
        this.showAlert("Failed to place bid. Please try again.", "danger");
      })
      .finally(() => {
        this.setButtonLoading(submitBtn, false);
      });
  }

  /**
   * Submit bid (mock implementation)
   */
  async submitBid(amount) {
    return new Promise((resolve, reject) => {
      setTimeout(() => {
        if (Math.random() > 0.1) {
          resolve({ success: true, bidId: Math.floor(Math.random() * 1000) });
        } else {
          reject(new Error("Bid submission failed"));
        }
      }, 1000);
    });
  }

  /**
   * Close bid modal
   */
  closeBidModal() {
    const modal = document.getElementById("bidModal");
    if (modal) {
      const bsModal = bootstrap.Modal.getInstance(modal);
      if (bsModal) {
        bsModal.hide();
      }
    }
  }

  /**
   * Initialize countdown timers for active and scheduled auctions
   */
  initializeCountdownTimers() {
    this.updateCountdowns();
    setInterval(() => {
      this.updateCountdowns();
    }, 1000); // Update every second for real-time countdown
  }

  /**
   * Update countdown timers for both live and scheduled auctions
   */
  updateCountdowns() {
    // Update end timers for live auctions
    const endTimers = document.querySelectorAll('[id^="timer-end-"]');
    endTimers.forEach((timer) => {
      const endTimeStr = timer.getAttribute("data-end-time");
      if (endTimeStr) {
        const endTime = new Date(endTimeStr);
        const now = new Date();
        const timeDiff = endTime - now;

        if (timeDiff > 0) {
          timer.textContent = this.formatTimeRemaining(timeDiff);
        } else {
          timer.textContent = "Ended";
          timer.parentElement.classList.add("bg-danger");
        }
      }
    });

    // Update start timers for scheduled auctions
    const startTimers = document.querySelectorAll('[id^="timer-start-"]');
    startTimers.forEach((timer) => {
      const startTimeStr = timer.getAttribute("data-start-time");
      if (startTimeStr) {
        const startTime = new Date(startTimeStr);
        const now = new Date();
        const timeDiff = startTime - now;

        if (timeDiff > 0) {
          timer.textContent = this.formatTimeRemaining(timeDiff);
        } else {
          timer.textContent = "Starting";
          timer.parentElement.classList.add("bg-success");
        }
      }
    });
  }

