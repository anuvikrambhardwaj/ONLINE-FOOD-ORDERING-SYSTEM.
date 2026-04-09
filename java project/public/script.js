let menu = [];
let cart = {};
let currentUser = JSON.parse(localStorage.getItem('swiggy_user_v3')) || null;
let currentCategory = 'All';

function showToast(message) {
    const container = document.getElementById('toast-container');
    const toast = document.createElement('div');
    toast.className = 'toast'; toast.innerText = message;
    container.appendChild(toast);
    setTimeout(() => { toast.style.opacity = '0'; setTimeout(() => toast.remove(), 300); }, 3000);
}

function updateNavState() {
    if (currentUser) {
        document.getElementById('nav-user-name').innerText = currentUser.name;
    } else {
        document.getElementById('nav-user-name').innerText = 'Login';
    }
}

async function fetchMenu() {
    const grid = document.getElementById('menu-grid');
    grid.innerHTML = '<div style="grid-column: 1/-1; text-align:center; padding: 3rem;">Loading immense catalog...</div>';
    try {
        const response = await fetch('/api/menu');
        menu = await response.json();
        renderMenu();
    } catch (error) {
        grid.innerHTML = '<div class="error">Failed to load menu.</div>';
    }
}

function filterCategory(catName) {
    currentCategory = catName;
    document.querySelectorAll('.cat-btn').forEach(btn => btn.classList.remove('active'));
    event.target.classList.add('active');
    document.getElementById('section-title').innerText = catName === 'All' ? 'All Items' : catName;
    renderMenu();
}

function renderMenu() {
    const grid = document.getElementById('menu-grid');
    grid.innerHTML = '';
    
    // Client side lightning-fast filtering for 100+ items
    const filtered = currentCategory === 'All' ? menu : menu.filter(m => m.category === currentCategory);
    
    if(filtered.length === 0) grid.innerHTML = '<div style="grid-column: 1/-1; text-align:center;">No items in this category.</div>';

    filtered.forEach(item => {
        const card = document.createElement('div'); card.className = 'menu-card';
        card.innerHTML = `
            <img src="${item.image_url}" class="food-img" alt="${item.name}" onerror="this.src='https://loremflickr.com/320/240/dish,food/all';" />
            <div class="food-info">
                <h3>${item.name}</h3>
                <p class="food-desc">${item.description}</p>
                <div class="food-footer">
                    <div class="price">₹${item.price.toFixed(2)}</div>
                    <button class="add-btn" onclick="addToCart(${item.id})">ADD</button>
                </div>
            </div>
        `;
        grid.appendChild(card);
    });
}

function addToCart(itemId) {
    if (cart[itemId]) cart[itemId].quantity += 1;
    else { const item = menu.find(m => m.id === itemId); cart[itemId] = { ...item, quantity: 1 }; }
    updateCartUI();
    showToast('Added to cart');
}

function updateQuantity(itemId, delta) {
    if (cart[itemId]) {
        cart[itemId].quantity += delta;
        if (cart[itemId].quantity <= 0) delete cart[itemId];
        updateCartUI();
    }
}

function updateCartUI() {
    const container = document.getElementById('cart-items');
    let totalItems = 0; let totalValue = 0;
    container.innerHTML = '';
    
    const itemIds = Object.keys(cart);
    if (itemIds.length === 0) {
        container.innerHTML = '<div class="empty-cart">Cart is empty</div>';
    } else {
        itemIds.forEach(id => {
            const item = cart[id];
            totalItems += item.quantity; totalValue += item.price * item.quantity;
            const div = document.createElement('div'); div.className = 'cart-item';
            div.innerHTML = `
                <div><div>${item.name}</div><div style="font-size: 0.8rem; color: gray;">₹${item.price.toFixed(2)} x ${item.quantity}</div></div>
                <div class="item-qty-ctrl">
                    <button class="qty-btn" onclick="updateQuantity(${id}, -1)">-</button><span>${item.quantity}</span><button class="qty-btn" onclick="updateQuantity(${id}, 1)">+</button>
                </div>
            `;
            container.appendChild(div);
        });
    }
    document.getElementById('cart-count').innerText = totalItems;
    document.getElementById('cart-total').innerText = `₹${totalValue.toFixed(2)}`;
}

function toggleCart() {
    document.getElementById('cart-sidebar').classList.toggle('open');
    document.getElementById('cart-overlay').classList.toggle('show');
}
function closeAll() {
    document.getElementById('cart-sidebar').classList.remove('open');
    document.getElementById('cart-overlay').classList.remove('show');
    document.getElementById('login-modal').classList.remove('show');
    document.getElementById('orders-modal').classList.remove('show');
}
function toggleUserDropdown() {
    // On mobile, tap triggers hover CSS. This function resolves ReferenceError for the HTML onClick.
}

// ------ DIRECT LOGIN LOGIC ------
async function directLogin() {
    const name = document.getElementById('login-name').value.trim();
    const phone = document.getElementById('login-phone').value.trim();
    if (!name || phone.length < 5) return showToast('Enter valid Name and Contact ID');
    
    try {
        const res = await fetch('/api/login', {
            method: 'POST', body: JSON.stringify({ name, phone })
        });
        const data = await res.json();
        if (data.success) {
            currentUser = { id: data.userId, name: data.name };
            localStorage.setItem('swiggy_user_v3', JSON.stringify(currentUser));
            updateNavState(); closeAll(); showToast('Logged in successfully!');
        } else {
            showToast('Login Failed');
        }
    } catch(e) { showToast('Server Error'); }
}

function cancelOtp() {} // Placeholder so UI checkout error doesn't break

// ------ CHECKOUT & TRACKING ------
async function checkout() {
    if (Object.keys(cart).length === 0) return showToast("Cart is empty");
    if (!currentUser) {
        document.getElementById('cart-overlay').classList.add('show');
        document.getElementById('login-modal').classList.add('show');
        cancelOtp(); // Reset modal state
        return;
    }
    
    const address = document.getElementById('delivery-address').value.trim();
    if(!address) {
        document.getElementById('delivery-address').style.borderColor = 'red';
        return showToast('Please enter Delivery Address');
    }
    
    let totalValue = 0;
    const items = Object.keys(cart).map(id => {
        totalValue += cart[id].price * cart[id].quantity;
        return { id: parseInt(id), quantity: cart[id].quantity };
    });

    const btn = document.getElementById('checkout-btn');
    btn.disabled = true; btn.innerText = "Processing...";
    
    try {
        const response = await fetch('/api/order', {
            method: 'POST',
            body: JSON.stringify({ userId: currentUser.id, total: totalValue, address: address, items: items })
        });
        const result = await response.json();
        if (result.success) {
            cart = {}; document.getElementById('delivery-address').value = '';
            updateCartUI(); toggleCart(); showToast('Order Placed! Open My Orders to Track.');
        }
    } catch (error) { showToast('Payment Failed'); } 
    finally { btn.disabled = false; btn.innerText = "Place Order"; }
}

function logout() {
    currentUser = null; localStorage.removeItem('swiggy_user_v3');
    updateNavState(); showToast("Logged out");
}

async function openMyOrders() {
    if (!currentUser) return showToast('Please login first');
    document.getElementById('cart-overlay').classList.add('show');
    document.getElementById('orders-modal').classList.add('show');

    const list = document.getElementById('orders-list');
    list.innerHTML = 'Fetching tracking history...';
    try {
        const res = await fetch('/api/orders?userId=' + currentUser.id + '&_t=' + new Date().getTime());
        const orders = await res.json();
        if (orders.length === 0) { list.innerHTML = 'No historic orders.'; return; }
        
        list.innerHTML = orders.map(o => `
            <div class="order-history-card">
                <div class="order-history-header">
                    <strong>Order #${o.id}</strong>
                    <span class="status-badge status-${o.status}">${o.status}</span>
                </div>
                <div style="font-size: 0.9rem; color: gray; margin-bottom: 10px;">Placed on: ${o.date}</div>
                <div style="margin-bottom: 10px; font-weight:600; font-size:0.95rem;">${o.items}</div>
                <div style="font-weight: bold; font-size:1.1rem; color: var(--primary);">₹${o.total.toFixed(2)}</div>
                
                <div class="tracking-box">
                    <div>📍 <strong>Address:</strong> ${o.address}</div>
                    <div style="margin-top: 5px;">👨‍🏍 <strong>Delivery Agent:</strong> ${o.boyName} </div>
                    <div style="margin-top: 5px;">📞 <strong>Contact:</strong> <a href="#" style="color:var(--primary);">${o.boyPhone}</a></div>
                </div>

                ${o.status === 'PENDING' ? `<button class="cancel-btn" onclick="cancelOrder(${o.id})">Cancel Order</button>` : ''}
            </div>
        `).join('');
    } catch(e) { list.innerHTML = 'Error loading history.'; }
}

async function cancelOrder(orderId) {
    if(!confirm("Cancel order and notify agent?")) return;
    try {
        await fetch('/api/cancel', { method: 'POST', body: JSON.stringify({ orderId }) });
        showToast('Order Cancelled Successfully');
        openMyOrders(); 
    } catch(e) { showToast('Failure'); }
}

updateNavState();
fetchMenu();
