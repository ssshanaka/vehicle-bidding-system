/* Footer Fragment JavaScript - Lanka Auto Traders */

// Footer-specific initialization
document.addEventListener("DOMContentLoaded", function () {
  initializeFooter();
});

// Initialize footer functionality
function initializeFooter() {
  // Initialize social links
  initializeSocialLinks();

  // Initialize footer links
  initializeFooterLinks();

  // Initialize contact information
  initializeContactInfo();

  // Update current date
  updateCurrentDate();

  // Initialize animations
  initializeFooterAnimations();
}

// Initialize social links with hover effects
function initializeSocialLinks() {
  const socialLinks = document.querySelectorAll(".social-link");

  socialLinks.forEach(function (link) {
    link.addEventListener("mouseenter", function () {
      this.style.transform = "translateY(-3px)";
    });

    link.addEventListener("mouseleave", function () {
      this.style.transform = "translateY(0)";
    });

    // Add click tracking
    link.addEventListener("click", function (e) {
      e.preventDefault();
      const platform = this.getAttribute("title") || "Social Media";
      console.log(`Social link clicked: ${platform}`);

      // You can add analytics tracking here
      // trackSocialClick(platform);
    });
  });
}

// Initialize footer links with smooth scrolling
function initializeFooterLinks() {
  const footerLinks = document.querySelectorAll(".footer-link");

  footerLinks.forEach(function (link) {
    link.addEventListener("click", function (e) {
      const href = this.getAttribute("href");

      // Handle internal links
      if (href && href.startsWith("#")) {
        e.preventDefault();
        const targetElement = document.querySelector(href);
        if (targetElement) {
          targetElement.scrollIntoView({
            behavior: "smooth",
            block: "start",
          });
        }
      }

      // Handle modal triggers
      if (href === "#" && this.getAttribute("data-bs-toggle") === "modal") {
        // Let Bootstrap handle the modal
        return;
      }
    });
  });
}

