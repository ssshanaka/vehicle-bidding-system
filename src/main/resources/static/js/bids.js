// Place a bid and notify server via AJAX
function placeBid(auctionId) {
    const bidAmount = prompt("Enter your bid amount:");
    if (!bidAmount) return;

    fetch(`/api/bids/${auctionId}`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ amount: parseFloat(bidAmount) })
    })
        .then(response => {
            if (!response.ok) throw new Error("Bid failed");
            return response.json();
        })
        .then(data => {
            alert("Bid placed successfully!");
        })
        .catch(err => alert(err.message));
}

// Optionally, subscribe to WebSocket updates for real-time bid changes
function subscribeToAuctionUpdates(auctionId) {
    const socket = new SockJS('/ws');
    const stompClient = Stomp.over(socket);

    stompClient.connect({}, () => {
        stompClient.subscribe('/topic/auctions/' + auctionId, (message) => {
            const updatedAuction = JSON.parse(message.body);
            const timerEl = document.getElementById('timer-' + auctionId);
            if (timerEl) startCountdown(timerEl.id, updatedAuction.currentEndTime);
        });
    });
}
