const API_BASE = window.ROAMLY_API_BASE || `${location.origin}/api/v1`;
const FALLBACK_PHOTOS = [
  "https://picsum.photos/seed/roamly-hotel-1/1200/820",
  "https://picsum.photos/seed/roamly-hotel-2/1200/820",
  "https://picsum.photos/seed/roamly-room-1/1000/760"
];

const state = {
  token: localStorage.getItem("roamly_token"),
  user: null,
  modal: null,
  authMode: "login",
  selectedHotel: null,
  selectedRoom: null,
  search: JSON.parse(sessionStorage.getItem("roamly_search") || "null") || defaultSearch(),
};

function defaultSearch() {
  const start = new Date(); start.setDate(start.getDate() + 7);
  const end = new Date(); end.setDate(end.getDate() + 10);
  return { city: "Goa", startDate: iso(start), endDate: iso(end), roomsCount: 1, page: 0, size: 12 };
}
function iso(d) { return d.toISOString().slice(0, 10); }
function money(value) { return new Intl.NumberFormat("en-IN", { style:"currency", currency:"INR", maximumFractionDigits:0 }).format(Number(value || 0)); }
function esc(value="") { return String(value).replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#039;'}[c])); }
function splitList(value="") { return value.split(/[\n,]/).map(v => v.trim()).filter(Boolean); }
function photo(item, index=0) { return item?.photos?.[index] || FALLBACK_PHOTOS[index % FALLBACK_PHOTOS.length]; }
function brandLogo() {
  return `<span class="brand-mark" aria-hidden="true"><svg viewBox="0 0 36 36" role="img"><path d="M18 4c7.4 0 13.4 5.7 13.4 12.8 0 8.2-8.2 14.2-13.4 15.6C12.8 31 4.6 25 4.6 16.8 4.6 9.7 10.6 4 18 4Z"/><path d="M12 18.5c4.9-7.1 10.9-7.1 15-5.9-2.1 7.2-7.1 10.8-14.7 10.5 3-2 5.8-3.3 9.9-4.1-3.5-.8-6.5-.5-10.2-.5Z"/></svg></span>`;
}
function roles() {
  if (!state.token) return [];
  try { return JSON.parse(atob(state.token.split(".")[1].replace(/-/g,"+").replace(/_/g,"/"))).roles || []; } catch { return []; }
}
function isManager() { return roles().includes("HOTEL_MANAGER"); }
function route() { return location.hash.slice(1) || "/"; }
function navigate(path) { location.hash = path; }

async function api(path, options={}, retry=true) {
  const headers = { ...(options.body ? {"Content-Type":"application/json"} : {}), ...(options.headers || {}) };
  if (state.token) headers.Authorization = `Bearer ${state.token}`;
  const response = await fetch(`${API_BASE}${path}`, { credentials:"include", ...options, headers });
  if (response.status === 401 && retry && state.token && !path.includes("/auth/")) {
    try {
      const refreshed = await api("/auth/refresh", { method:"POST" }, false);
      setToken(refreshed.accessToken);
      return api(path, options, false);
    } catch { logout(false); throw new Error("Your session expired. Please sign in again."); }
  }
  let payload = null;
  if (response.status !== 204) {
    const text = await response.text();
    payload = text ? JSON.parse(text) : null;
  }
  if (!response.ok) throw new Error(payload?.apiError?.message || payload?.message || "Something went wrong");
  return payload?.data ?? payload;
}
function setToken(token) { state.token = token; token ? localStorage.setItem("roamly_token", token) : localStorage.removeItem("roamly_token"); }
async function loadUser() {
  if (!state.token) return null;
  try { state.user = await api("/users/profile"); return state.user; }
  catch { setToken(null); state.user = null; return null; }
}
async function logout(refresh=true) {
  try { await api("/auth/logout", {method:"POST"}, false); } catch {}
  setToken(null); state.user = null;
  if (refresh) { toast("You’re signed out"); navigate("/"); render(); }
}
function toast(message, type="") {
  const el = document.createElement("div"); el.className = `toast ${type}`; el.textContent = message;
  document.querySelector("#toast-root").append(el); setTimeout(() => el.remove(), 3500);
}
function setLoading(button, loading, label="Working…") {
  if (!button) return; if (loading) { button.dataset.label=button.innerHTML; button.innerHTML=label; button.disabled=true; }
  else { button.innerHTML=button.dataset.label || button.innerHTML; button.disabled=false; }
}

function nav() {
  const current = route();
  return `<nav class="nav"><div class="container nav-inner">
    <a class="brand" href="#/">${brandLogo()}<span>roamly</span></a>
    <div class="nav-links">
      <a href="#/" class="${current==="/"?"active":""}">Discover</a>
      <a href="#/search" class="${current.startsWith("/search")?"active":""}">Stays</a>
      ${state.user ? `<a href="#/trips" class="${current.startsWith("/trips")?"active":""}">My trips</a>` : ""}
      ${isManager() ? `<a href="#/admin" class="${current.startsWith("/admin")?"active":""}">Host dashboard</a>` : ""}
    </div>
    <div class="nav-actions">
      ${state.user ? `<button class="btn btn-outline btn-sm" onclick="navigate('${isManager()?"/admin":"/profile"}')">${esc(state.user.name?.split(" ")[0] || "Account")}</button>
        <button class="icon-btn" title="Sign out" onclick="logout()">↗</button>`
      : `<button class="btn btn-outline btn-sm" onclick="openAuth('manager')">List your place</button>
         <button class="btn btn-dark btn-sm" onclick="openAuth('login')">Sign in</button>`}
    </div>
  </div></nav>`;
}
function footer() {
  return `<footer class="footer"><div class="container footer-inner"><div>© ${new Date().getFullYear()} Roamly. Made for beautiful detours.</div><div>Privacy · Terms · Support</div></div></footer>`;
}
function layout(content, includeFooter=true) {
  return `<div class="shell">${nav()}${content}${includeFooter?footer():""}<div id="modal-root"></div></div>`;
}
function searchForm(compact=false) {
  const s=state.search;
  return `<form class="${compact?"compact-search":"search-dock"}" onsubmit="submitSearch(event)">
    <div class="field"><label>Where</label><input name="city" value="${esc(s.city)}" placeholder="Try Goa or Jaipur" required></div>
    <div class="field"><label>Check in</label><input type="date" name="startDate" min="${iso(new Date())}" value="${s.startDate}" required></div>
    <div class="field"><label>Check out</label><input type="date" name="endDate" min="${s.startDate}" value="${s.endDate}" required></div>
    <div class="field"><label>Rooms</label><select name="roomsCount">${[1,2,3,4,5].map(n=>`<option ${s.roomsCount==n?"selected":""}>${n}</option>`).join("")}</select></div>
    <button class="search-button" aria-label="Search">⌕</button>
  </form>`;
}
function submitSearch(event) {
  event.preventDefault(); const data=Object.fromEntries(new FormData(event.target));
  if (data.endDate <= data.startDate) return toast("Check-out must be after check-in", "error");
  state.search={...state.search,...data,roomsCount:Number(data.roomsCount),page:0,size:12};
  sessionStorage.setItem("roamly_search",JSON.stringify(state.search)); navigate("/search");
  if (route()==="/search") render();
}

function homePage() {
  return layout(`<main>
    <section class="hero"><div class="container"><div class="hero-card">
      <div class="hero-copy"><div class="eyebrow">Stay somewhere worth remembering</div>
        <h1>The good kind<br>of getting lost.</h1>
        <p>Thoughtful homes, soulful hotels, and wild little hideaways—handpicked for the stories you’ll tell later.</p>
      </div>${searchForm()}</div></div></section>
    <section class="section"><div class="container">
      <div class="section-head"><div><div class="eyebrow page-kicker">Live from the database</div><h2>Places with a pulse</h2></div><p>Distinctive spaces, generous hosts, and settings that make it delightfully difficult to leave.</p></div>
      <div id="featured-stays" class="loading-grid">${"<div class='skeleton'></div>".repeat(3)}</div>
    </div></section>
    <section class="section alt"><div class="container">
      <div class="section-head"><div><div class="eyebrow">Travel, gently</div><h2>Less searching.<br>More feeling.</h2></div><p>We designed every step to feel simple, considered, and human—from first spark to front door.</p></div>
      <div class="feature-grid">
        <div class="feature"><div class="feature-num">01</div><h3>Find your rhythm</h3><p>Search around the way you want to feel, then narrow down without losing the magic.</p></div>
        <div class="feature"><div class="feature-num">02</div><h3>Book with clarity</h3><p>Real availability, clear pricing, and the details that actually matter before you arrive.</p></div>
        <div class="feature"><div class="feature-num">03</div><h3>Carry less worry</h3><p>Your stays, guests, and plans live together, exactly where you expect to find them.</p></div>
      </div>
    </div></section>
  </main>`);
}
async function loadFeaturedStays() {
  const root=document.querySelector("#featured-stays");
  if(!root) return;
  const featuredSearch={...defaultSearch(), city:state.search.city || "Goa", size:6, page:0};
  try {
    let result=await api("/hotels/search",{method:"POST",body:JSON.stringify(featuredSearch)});
    let stays=result?.content || [];
    if(!stays.length && featuredSearch.city!=="Goa") {
      result=await api("/hotels/search",{method:"POST",body:JSON.stringify({...featuredSearch, city:"Goa"})});
      stays=result?.content || [];
    }
    root.outerHTML=stays.length
      ? `<div class="stay-grid">${stays.slice(0,6).map((x,i)=>stayCard(x,null,i)).join("")}</div>`
      : `<div class="empty"><div class="empty-icon">⌁</div><h2>No live stays yet</h2><p class="muted">Add rooms, open inventory, and activate a property to feature it here.</p></div>`;
  } catch(e) {
    root.outerHTML=errorState(e.message);
  }
}
function stayCard(hotelPrice, directPrice, index=0) {
  const h=hotelPrice.hotel || hotelPrice; const price=hotelPrice.price || directPrice;
  return `<article class="stay-card" ${h.id?`onclick="navigate('/hotel/${h.id}')"`:""}>
    <div class="stay-photo"><img src="${esc(photo(h,index))}" alt="${esc(h.name)}"><span class="pill">${h.active===false?"Coming soon":"Guest favourite"}</span></div>
    <div class="stay-body"><div class="stay-line"><div><h3 class="stay-title">${esc(h.name)}</h3><div class="muted small">${esc(h.city)}</div></div><div>★ 4.${8-index%3}</div></div>
      <div class="price">${money(price)} <span>/ night</span></div></div>
  </article>`;
}

async function searchPage() {
  document.querySelector("#app").innerHTML=layout(`<main class="page"><div class="container">
    <div class="page-head"><div class="eyebrow page-kicker">Curated for your dates</div><h1>Stays in ${esc(state.search.city)}</h1></div>
    ${searchForm(true)}<div class="loading-grid">${"<div class='skeleton'></div>".repeat(6)}</div>
  </div></main>`);
  try {
    const result=await api("/hotels/search",{method:"POST",body:JSON.stringify(state.search)});
    const stays=result?.content || [];
    document.querySelector(".loading-grid").outerHTML=stays.length
      ? `<div class="stay-grid">${stays.map((x,i)=>stayCard(x,null,i)).join("")}</div>`
      : `<div class="empty"><div class="empty-icon">⌁</div><h2>No stays surfaced just yet</h2><p class="muted">Try another city or shift your dates a little.</p></div>`;
  } catch(e) { document.querySelector(".loading-grid").outerHTML=errorState(e.message); }
}
function errorState(message) { return `<div class="empty"><div class="empty-icon">!</div><h2>We hit a small detour</h2><p class="muted">${esc(message)}</p><button class="btn btn-dark" onclick="render()">Try again</button></div>`; }

async function hotelPage(id) {
  document.querySelector("#app").innerHTML=layout(`<main class="page"><div class="container"><div class="skeleton"></div></div></main>`);
  try {
    const info=await api(`/hotels/${id}/info`); state.selectedHotel=info.hotelDto;
    const h=info.hotelDto, rooms=info.rooms || [], pics=[photo(h,0),photo(h,1),photo(h,2)];
    document.querySelector("#app").innerHTML=layout(`<main class="page"><div class="container">
      <div class="gallery"><img class="main-photo" src="${esc(pics[0])}" alt="${esc(h.name)}"><img src="${esc(pics[1])}" alt=""><img src="${esc(pics[2])}" alt=""></div>
      <div class="detail-grid"><section class="detail-copy">
        <div class="eyebrow page-kicker">${esc(h.city)}</div><h1>${esc(h.name)}</h1>
        <p class="muted">${esc(h.contactInfo?.completeAddress || "A remarkable stay in the heart of "+h.city)} · ★ 4.9</p>
        <div class="amenities">${(h.amenities||[]).map(a=>`<span class="amenity">✓ ${esc(a)}</span>`).join("")}</div>
        <h2>Choose your room</h2>
        ${rooms.length?rooms.map(r=>`<div class="room-card">
          <img src="${esc(photo(r,1))}" alt="${esc(r.type)}"><div><b>${esc(r.type)}</b><div class="muted small">Sleeps ${r.capacity} · ${r.totalCount} available</div><div class="price">${money(r.basePrice)} <span>/ night</span></div></div>
          <button class="btn btn-outline btn-sm" onclick='selectRoom(${JSON.stringify(r)})'>Select</button></div>`).join(""):`<p class="muted">Room details are being prepared.</p>`}
      </section>
      <aside><div class="booking-card"><h3>Your stay, at a glance</h3>
        <div class="input"><label>Room</label><select id="booking-room" onchange="chooseRoom(this.value)"><option value="">Choose a room</option>${rooms.map(r=>`<option value="${r.id}">${esc(r.type)} — ${money(r.basePrice)}</option>`).join("")}</select></div>
        <div class="form-grid"><div class="input"><label>Check in</label><input id="book-in" type="date" value="${state.search.startDate}"></div><div class="input"><label>Check out</label><input id="book-out" type="date" value="${state.search.endDate}"></div></div>
        <div class="input"><label>Rooms</label><select id="book-count">${[1,2,3,4,5].map(n=>`<option ${state.search.roomsCount==n?"selected":""}>${n}</option>`).join("")}</select></div>
        <div class="total-line"><span>From</span><span id="booking-price">Select a room</span></div>
        <button class="btn btn-accent btn-block" onclick="startBooking()">Reserve your stay</button>
        <p class="muted small" style="text-align:center">You won’t be charged yet</p>
      </div></aside></div>
    </div></main>`);
    window._rooms=rooms;
  } catch(e) { document.querySelector("#app").innerHTML=layout(`<main class="page"><div class="container">${errorState(e.message)}</div></main>`); }
}
function selectRoom(room) { state.selectedRoom=room; const select=document.querySelector("#booking-room"); if(select) select.value=room.id; updateBookingPrice(); document.querySelector(".booking-card")?.scrollIntoView({behavior:"smooth"}); }
function chooseRoom(id) { state.selectedRoom=(window._rooms||[]).find(r=>String(r.id)===String(id)); updateBookingPrice(); }
function updateBookingPrice() { const el=document.querySelector("#booking-price"); if(el) el.textContent=state.selectedRoom?`${money(state.selectedRoom.basePrice)} / night`:"Select a room"; }
async function startBooking() {
  if (!state.user) return openAuth("login", "Sign in to reserve this stay");
  if (!state.selectedRoom) return toast("Choose a room first","error");
  const checkInDate=document.querySelector("#book-in").value,checkOutDate=document.querySelector("#book-out").value,roomsCount=Number(document.querySelector("#book-count").value);
  if(checkOutDate<=checkInDate) return toast("Check-out must be after check-in","error");
  try {
    const booking=await api("/bookings/init",{method:"POST",body:JSON.stringify({hotelId:state.selectedHotel.id,roomId:state.selectedRoom.id,checkInDate,checkOutDate,roomsCount})});
    openGuestModal(booking);
  } catch(e) { toast(e.message,"error"); }
}
function openGuestModal(booking) {
  state.modal="guests"; renderModal(`<div class="modal modal-lg"><div class="modal-head"><div><div class="eyebrow page-kicker">Almost yours</div><h2>Who’s checking in?</h2></div><button class="icon-btn" onclick="closeModal()">×</button></div>
    <p class="muted">Add at least one guest. Your room is held for ten minutes while you finish.</p>
    <div id="saved-guest-suggestions" class="guest-suggestions"><span class="muted small">Checking saved guests…</span></div>
    <form onsubmit="continueToPayment(event,${booking.id})"><div id="guest-fields">
      ${guestFields(0)}
    </div><button type="button" class="btn btn-outline btn-sm" onclick="addGuestFields()">+ Add another guest</button>
    <div class="total-line"><span>Stay total</span><span>${money(booking.amount)}</span></div>
    <button class="btn btn-accent btn-block">Continue to secure payment</button></form></div>`);
  loadSavedGuestSuggestions();
}
function guestFields(i) { return `<div class="form-grid guest-set" style="margin-bottom:14px"><div class="input"><label>Guest name</label><input name="name" required></div><div class="input"><label>Age</label><input name="age" type="number" min="1" max="120" required></div><div class="input"><label>Gender</label><select name="gender"><option>MALE</option><option>FEMALE</option><option>OTHER</option></select></div></div>`; }
function addGuestFields(){ document.querySelector("#guest-fields").insertAdjacentHTML("beforeend",guestFields(document.querySelectorAll(".guest-set").length)); }
async function loadSavedGuestSuggestions() {
  const root=document.querySelector("#saved-guest-suggestions");
  if(!root) return;
  try {
    const guests=await api("/users/guests");
    window._savedGuestSuggestions=guests;
    root.innerHTML=guests.length
      ? `<div class="guest-suggestion-title">Use saved guest info</div><div class="guest-chip-row">${guests.map((g,i)=>`<button type="button" class="guest-chip" onclick="useSavedGuest(${i})">${esc(g.name)} <span>${g.age} · ${esc(g.gender)}</span></button>`).join("")}</div>`
      : `<span class="muted small">No saved guests yet — fill one below and it will be saved for next time.</span>`;
  } catch {
    root.innerHTML=`<span class="muted small">Saved guests could not be loaded. You can still enter guests manually.</span>`;
  }
}
function useSavedGuest(index) {
  const guest=(window._savedGuestSuggestions||[])[index];
  if(!guest) return;
  let target=[...document.querySelectorAll(".guest-set")].find(set=>!set.querySelector('[name=name]').value.trim());
  if(!target) {
    addGuestFields();
    target=[...document.querySelectorAll(".guest-set")].at(-1);
  }
  target.querySelector('[name=name]').value=guest.name || "";
  target.querySelector('[name=age]').value=guest.age || "";
  target.querySelector('[name=gender]').value=guest.gender || "OTHER";
  toast(`${guest.name} added to this booking`);
}
async function continueToPayment(event,id) {
  event.preventDefault(); const btn=event.submitter; setLoading(btn,true,"Preparing checkout…");
  const guests=[...event.target.querySelectorAll(".guest-set")].map(set=>({name:set.querySelector('[name=name]').value,age:Number(set.querySelector('[name=age]').value),gender:set.querySelector('[name=gender]').value}));
  try { await api(`/bookings/${id}/addGuests`,{method:"POST",body:JSON.stringify(guests)}); const payment=await api(`/bookings/${id}/payments`,{method:"POST"}); location.href=payment.sessionUrl; }
  catch(e){ toast(e.message,"error"); setLoading(btn,false); }
}

function openAuth(mode="login", title="Welcome back") { state.authMode=mode; state.modal="auth"; renderAuth(title); }
function renderAuth(title) {
  const manager=state.authMode==="manager", signup=state.authMode==="signup"||manager;
  renderModal(`<div class="modal"><div class="modal-head"><div><div class="eyebrow page-kicker">${manager?"Roamly for hosts":"Your next stay starts here"}</div><h2>${manager?"Open your host account":signup?"Join Roamly":title}</h2></div><button class="icon-btn" onclick="closeModal()">×</button></div>
    <form onsubmit="submitAuth(event,'${state.authMode}')">
      ${signup?`<div class="input"><label>Full name</label><input name="name" autocomplete="name" required></div><br>`:""}
      <div class="input"><label>Email</label><input name="email" type="email" autocomplete="email" required></div><br>
      <div class="input"><label>Password</label><input name="password" type="password" minlength="6" autocomplete="${signup?"new-password":"current-password"}" required></div><br>
      <button class="btn btn-dark btn-block">${signup?"Create account":"Sign in"}</button>
    </form>
    ${manager?"":`<div class="divider">or</div><button class="google-btn" onclick="googleLogin()"><span style="color:#4285f4">G</span>&nbsp; Continue with Google</button>`}
    <div class="auth-switch">${signup?"Already have an account?":"New around here?"} <button class="text-btn" onclick="state.authMode='${signup?"login":"signup"}';renderAuth('${signup?"Welcome back":"Join Roamly"}')">${signup?"Sign in":"Create one"}</button></div>
  </div>`);
}
async function submitAuth(event, mode) {
  event.preventDefault(); const btn=event.submitter; setLoading(btn,true);
  const values=Object.fromEntries(new FormData(event.target));
  try {
    if(mode==="signup"||mode==="manager") {
      await api(`/auth/signup${mode==="manager"?"/manager":""}`,{method:"POST",body:JSON.stringify(values)});
    }
    const login=await api("/auth/login",{method:"POST",body:JSON.stringify({email:values.email,password:values.password})});
    setToken(login.accessToken); await loadUser(); closeModal(); toast(mode==="login"?"Welcome back":"Your account is ready");
    navigate(isManager()?"/admin":route()); render();
  } catch(e) { toast(e.message,"error"); setLoading(btn,false); }
}
function googleLogin() { location.href=`${API_BASE}/oauth2/authorization/google`; }
function renderModal(html) { const root=document.querySelector("#modal-root"); if(root) root.innerHTML=`<div class="modal-backdrop" onclick="if(event.target===this)closeModal()">${html}</div>`; }
function closeModal(){ state.modal=null; const root=document.querySelector("#modal-root"); if(root) root.innerHTML=""; }

async function tripsPage() {
  if(!state.user) return requireAuth();
  document.querySelector("#app").innerHTML=layout(`<main class="page"><div class="container"><div class="page-head"><div class="eyebrow page-kicker">Your travel story</div><h1>Trips</h1></div><div class="skeleton"></div></div></main>`);
  try {
    const bookings=await api("/users/myBookings");
    document.querySelector(".skeleton").outerHTML=bookings.length?bookings.map(b=>`<div class="trip-card"><div><span class="status ${b.bookingStatus?.toLowerCase()}">${esc(b.bookingStatus)}</span><h3>${formatDate(b.checkInDate)} → ${formatDate(b.checkOutDate)}</h3><div class="muted">${b.roomCount} room${b.roomCount>1?"s":""} · ${b.guests?.length||0} guests · Booking #${b.id}</div></div><div><b>${money(b.amount)}</b>${b.bookingStatus==="CONFIRMED"?`<br><button class="btn btn-danger btn-sm" style="margin-top:12px" onclick="cancelBooking(${b.id})">Cancel</button>`:""}</div></div>`).join("")
      : `<div class="empty"><div class="empty-icon">⌁</div><h2>Your suitcase is suspiciously light</h2><p class="muted">When you book a stay, it’ll appear here.</p><button class="btn btn-dark" onclick="navigate('/search')">Find a stay</button></div>`;
  } catch(e){ document.querySelector(".skeleton").outerHTML=errorState(e.message); }
}
function formatDate(d){ return new Date(`${d}T00:00:00`).toLocaleDateString("en-IN",{day:"numeric",month:"short",year:"numeric"}); }
async function cancelBooking(id){ if(!confirm("Cancel this confirmed booking and request a refund?"))return; try{await api(`/bookings/${id}/cancel`,{method:"POST"});toast("Booking cancelled");tripsPage();}catch(e){toast(e.message,"error");} }

async function profilePage() {
  if(!state.user)return requireAuth();
  document.querySelector("#app").innerHTML=layout(`<main class="page"><div class="container"><div class="page-head"><div class="eyebrow page-kicker">The details that travel with you</div><h1>Your profile</h1></div>
    <div class="profile-grid"><section class="panel"><div class="panel-head"><h2>Personal details</h2></div>
      <form onsubmit="saveProfile(event)"><div class="input"><label>Name</label><input name="name" value="${esc(state.user.name)}" required></div><br>
      <div class="input"><label>Date of birth</label><input name="dateOfBirth" type="date" value="${state.user.dateOfBirth||""}"></div><br>
      <div class="input"><label>Gender</label><select name="gender"><option value="">Prefer not to say</option>${["MALE","FEMALE","OTHER"].map(x=>`<option ${state.user.gender===x?"selected":""}>${x}</option>`).join("")}</select></div><br>
      <button class="btn btn-dark">Save changes</button></form></section>
    <section class="panel"><div class="panel-head"><h2>Saved guests</h2><button class="btn btn-outline btn-sm" onclick="openGuestEditor()">+ Add</button></div><div id="guest-list">Loading…</div></section></div>
  </div></main>`); loadGuests();
}
async function saveProfile(e){e.preventDefault();const data=Object.fromEntries(new FormData(e.target));if(!data.gender)data.gender=null;try{await api("/users/profile",{method:"PATCH",body:JSON.stringify(data)});await loadUser();toast("Profile updated");}catch(err){toast(err.message,"error");}}
async function loadGuests(){try{const guests=await api("/users/guests");document.querySelector("#guest-list").innerHTML=guests.length?guests.map(g=>`<div class="hotel-row"><div class="brand-mark">${esc(g.name?.[0]||"G")}</div><div><b>${esc(g.name)}</b><div class="muted small">${g.age} years · ${esc(g.gender)}</div></div><div class="row-actions"><button class="btn btn-danger btn-sm" onclick="deleteGuest(${g.id})">Remove</button></div></div>`).join(""):`<p class="muted">No saved guests yet.</p>`;}catch(e){document.querySelector("#guest-list").innerHTML=esc(e.message);}}
function openGuestEditor(){renderModal(`<div class="modal"><div class="modal-head"><h2>Add a guest</h2><button class="icon-btn" onclick="closeModal()">×</button></div><form onsubmit="saveGuest(event)"><div class="input"><label>Name</label><input name="name" required></div><br><div class="form-grid"><div class="input"><label>Age</label><input name="age" type="number" min="1" required></div><div class="input"><label>Gender</label><select name="gender"><option>MALE</option><option>FEMALE</option><option>OTHER</option></select></div></div><br><button class="btn btn-dark btn-block">Save guest</button></form></div>`);}
async function saveGuest(e){e.preventDefault();const d=Object.fromEntries(new FormData(e.target));d.age=Number(d.age);try{await api("/users/guests",{method:"POST",body:JSON.stringify(d)});closeModal();loadGuests();toast("Guest saved");}catch(err){toast(err.message,"error");}}
async function deleteGuest(id){try{await api(`/users/guests/${id}`,{method:"DELETE"});loadGuests();toast("Guest removed");}catch(e){toast(e.message,"error");}}

async function adminPage() {
  if(!state.user||!isManager())return requireManager();
  document.querySelector("#app").innerHTML=layout(`<main class="page"><div class="container"><div class="dashboard-layout">${adminSidebar("properties")}<section class="dash-main"><div class="page-head-row page-head"><div><div class="eyebrow page-kicker">Host command centre</div><h1>Your properties</h1></div><button class="btn btn-accent" onclick="openHotelEditor()">+ Add property</button></div><div id="admin-content"><div class="skeleton"></div></div></section></div></div></main>`);
  try {
    const hotels=await api("/admin/hotels"); window._hotels=hotels;
    const active=hotels.filter(h=>h.active).length;
    document.querySelector("#admin-content").innerHTML=`<div class="stat-grid"><div class="stat"><span class="muted small">Properties</span><b>${hotels.length}</b></div><div class="stat"><span class="muted small">Live listings</span><b>${active}</b></div><div class="stat"><span class="muted small">Drafts</span><b>${hotels.length-active}</b></div></div>
    <div class="panel"><div class="panel-head"><h2>Portfolio</h2></div>${hotels.length?hotels.map(h=>hotelAdminRow(h)).join(""):`<div class="empty"><h3>Your first property belongs here</h3><button class="btn btn-dark" onclick="openHotelEditor()">Add property</button></div>`}</div>`;
  }catch(e){document.querySelector("#admin-content").innerHTML=errorState(e.message);}
}
function adminSidebar(active){return `<aside class="sidebar"><div class="sidebar-title">Roamly host</div><button class="side-link ${active==="properties"?"active":""}" onclick="navigate('/admin')">⌂ &nbsp;Properties</button><button class="side-link" onclick="navigate('/profile')">○ &nbsp;Profile</button><button class="side-link" onclick="logout()">↗ &nbsp;Sign out</button></aside>`;}
function hotelAdminRow(h){return `<div class="hotel-row"><img src="${esc(photo(h))}" alt=""><div><b>${esc(h.name)}</b><div class="muted small">${esc(h.city)} · <span class="status ${h.active?"active":""}">${h.active?"Live":"Draft"}</span></div></div><div class="row-actions">${!h.active?`<button class="btn btn-accent btn-sm" onclick="activateHotel(${h.id})">Go live</button>`:""}<button class="btn btn-outline btn-sm" onclick="navigate('/admin/hotel/${h.id}')">Manage</button><button class="btn btn-outline btn-sm" onclick="openHotelEditorById(${h.id})">Edit</button><button class="btn btn-danger btn-sm" onclick="deleteHotel(${h.id})">Delete</button></div></div>`;}
function openHotelEditorById(id){openHotelEditor((window._hotels||[]).find(h=>h.id===id));}
function openHotelEditor(h=null){renderModal(`<div class="modal modal-lg"><div class="modal-head"><div><div class="eyebrow page-kicker">Property details</div><h2>${h?"Edit property":"Add a new property"}</h2></div><button class="icon-btn" onclick="closeModal()">×</button></div>
  <form onsubmit="saveHotel(event,${h?.id||"null"})"><div class="form-grid"><div class="input"><label>Name</label><input name="name" value="${esc(h?.name||"")}" required></div><div class="input"><label>City</label><input name="city" value="${esc(h?.city||"")}" required></div>
  <div class="input"><label>Contact email</label><input type="email" name="email" value="${esc(h?.contactInfo?.email||"")}" required></div><div class="input"><label>Phone</label><input name="phoneNumber" value="${esc(h?.contactInfo?.phoneNumber||"")}"></div>
  <div class="input"><label>Complete address</label><textarea name="completeAddress" required>${esc(h?.contactInfo?.completeAddress||"")}</textarea></div><div class="input"><label>Map location / landmark</label><textarea name="location" required>${esc(h?.contactInfo?.location||"")}</textarea></div>
  <div class="input"><label>Amenities (comma separated)</label><textarea name="amenities">${esc((h?.amenities||[]).join(", "))}</textarea></div><div class="input"><label>Photo URLs (one per line)</label><textarea name="photos">${esc((h?.photos||[]).join("\n"))}</textarea></div></div>
  <div class="form-actions"><button type="button" class="btn btn-outline" onclick="closeModal()">Cancel</button><button class="btn btn-dark">Save property</button></div></form></div>`);}
async function saveHotel(e,id){e.preventDefault();const d=Object.fromEntries(new FormData(e.target));const body={name:d.name,city:d.city,photos:splitList(d.photos),amenities:splitList(d.amenities),contactInfo:{email:d.email,phoneNumber:d.phoneNumber,completeAddress:d.completeAddress,location:d.location},active:id?(window._hotels||[]).find(h=>h.id===id)?.active:false};try{await api(`/admin/hotels${id?"/"+id:""}`,{method:id?"PUT":"POST",body:JSON.stringify(body)});closeModal();toast(id?"Property updated":"Property added");adminPage();}catch(err){toast(err.message,"error");}}
async function activateHotel(id){try{await api(`/admin/hotels/${id}/activate`,{method:"PATCH"});toast("Your property is live");adminPage();}catch(e){toast(e.message,"error");}}
async function deleteHotel(id){if(!confirm("Delete this property, its rooms, and future inventory?"))return;try{await api(`/admin/hotels/${id}`,{method:"DELETE"});toast("Property deleted");adminPage();}catch(e){toast(e.message,"error");}}

async function adminHotelPage(id){
  if(!state.user||!isManager())return requireManager();
  document.querySelector("#app").innerHTML=layout(`<main class="page"><div class="container"><div class="dashboard-layout">${adminSidebar("properties")}<section class="dash-main"><div class="skeleton"></div></section></div></div></main>`);
  try{
    const [hotel,rooms,bookings,report]=await Promise.all([api(`/admin/hotels/${id}`),api(`/admin/hotels/${id}/rooms`),api(`/admin/hotels/${id}/bookings`),api(`/admin/hotels/${id}/reports`)]);
    window._adminHotel=hotel;window._rooms=rooms;
    document.querySelector(".dash-main").innerHTML=`<div class="page-head-row page-head"><div><button class="text-btn" onclick="navigate('/admin')">← All properties</button><h1>${esc(hotel.name)}</h1><div class="muted">${esc(hotel.city)} · <span class="status ${hotel.active?"active":""}">${hotel.active?"Live":"Draft"}</span></div></div><button class="btn btn-accent" onclick="openRoomEditor(${id})">+ Add room</button></div>
    <div class="stat-grid"><div class="stat"><span class="muted small">Confirmed bookings</span><b>${report.bookingCount||0}</b></div><div class="stat"><span class="muted small">Total revenue</span><b>${money(report.totalRevenue)}</b></div><div class="stat"><span class="muted small">Average booking</span><b>${money(report.avgRevenue)}</b></div></div>
    <div class="panel"><div class="panel-head"><h2>Rooms & inventory</h2></div>${rooms.length?rooms.map(r=>roomAdminRow(id,r)).join(""):`<p class="muted">Add a room type to begin managing availability.</p>`}</div>
    <div class="panel"><div class="panel-head"><h2>Recent bookings</h2></div>${bookings.length?`<div class="inventory-table"><table><thead><tr><th>Booking</th><th>Dates</th><th>Rooms</th><th>Amount</th><th>Status</th></tr></thead><tbody>${bookings.map(b=>`<tr><td>#${b.id}</td><td>${formatDate(b.checkInDate)} – ${formatDate(b.checkOutDate)}</td><td>${b.roomCount}</td><td>${money(b.amount)}</td><td><span class="status ${b.bookingStatus?.toLowerCase()}">${b.bookingStatus}</span></td></tr>`).join("")}</tbody></table></div>`:`<p class="muted">No bookings yet.</p>`}</div>`;
  }catch(e){document.querySelector(".dash-main").innerHTML=errorState(e.message);}
}
function roomAdminRow(hotelId,r){return `<div class="hotel-row"><img src="${esc(photo(r,1))}" alt=""><div><b>${esc(r.type)}</b><div class="muted small">${money(r.basePrice)} · ${r.totalCount} rooms · Sleeps ${r.capacity}</div></div><div class="row-actions"><button class="btn btn-accent btn-sm" onclick="openInventoryById(${r.id})">Inventory</button><button class="btn btn-outline btn-sm" onclick="openRoomEditorById(${hotelId},${r.id})">Edit</button><button class="btn btn-danger btn-sm" onclick="deleteRoom(${hotelId},${r.id})">Delete</button></div></div>`;}
function openRoomEditorById(hotelId,roomId){openRoomEditor(hotelId,(window._rooms||[]).find(r=>r.id===roomId));}
function openInventoryById(roomId){const room=(window._rooms||[]).find(r=>r.id===roomId);openInventory(roomId,room?.type||"Room inventory");}
function openRoomEditor(hotelId,r=null){renderModal(`<div class="modal modal-lg"><div class="modal-head"><h2>${r?"Edit room":"Add room"}</h2><button class="icon-btn" onclick="closeModal()">×</button></div><form onsubmit="saveRoom(event,${hotelId},${r?.id||"null"})"><div class="form-grid"><div class="input"><label>Room type</label><input name="type" value="${esc(r?.type||"")}" placeholder="Garden suite" required></div><div class="input"><label>Base price / night</label><input name="basePrice" type="number" min="1" value="${r?.basePrice||""}" required></div><div class="input"><label>Total room count</label><input name="totalCount" type="number" min="1" value="${r?.totalCount||1}" required></div><div class="input"><label>Guest capacity</label><input name="capacity" type="number" min="1" value="${r?.capacity||2}" required></div><div class="input"><label>Amenities</label><textarea name="amenities">${esc((r?.amenities||[]).join(", "))}</textarea></div><div class="input"><label>Photo URLs</label><textarea name="photos">${esc((r?.photos||[]).join("\n"))}</textarea></div></div><div class="form-actions"><button class="btn btn-dark">Save room</button></div></form></div>`);}
async function saveRoom(e,hotelId,roomId){e.preventDefault();const d=Object.fromEntries(new FormData(e.target));const body={type:d.type,roomType:"DELUXE",basePrice:Number(d.basePrice),totalCount:Number(d.totalCount),capacity:Number(d.capacity),amenities:splitList(d.amenities),photos:splitList(d.photos)};try{await api(`/admin/hotels/${hotelId}/rooms${roomId?"/"+roomId:""}`,{method:roomId?"PUT":"POST",body:JSON.stringify(body)});closeModal();toast("Room saved");adminHotelPage(hotelId);}catch(err){toast(err.message,"error");}}
async function deleteRoom(hotelId,roomId){if(!confirm("Delete this room type and its future inventory?"))return;try{await api(`/admin/hotels/${hotelId}/rooms/${roomId}`,{method:"DELETE"});toast("Room deleted");adminHotelPage(hotelId);}catch(e){toast(e.message,"error");}}
async function openInventory(roomId,type){renderModal(`<div class="modal modal-lg"><div class="modal-head"><div><div class="eyebrow page-kicker">Availability calendar</div><h2>${esc(type)}</h2></div><button class="icon-btn" onclick="closeModal()">×</button></div><div id="inventory-content"><div class="skeleton"></div></div></div>`);try{const items=await api(`/admin/inventory/rooms/${roomId}`);document.querySelector("#inventory-content").innerHTML=`<form onsubmit="updateInventory(event,${roomId},'${esc(type)}')"><div class="form-grid"><div class="input"><label>From</label><input name="startDate" type="date" value="${iso(new Date())}" required></div><div class="input"><label>To</label><input name="endDate" type="date" value="${iso(new Date(Date.now()+6*86400000))}" required></div><div class="input"><label>Price multiplier</label><input name="surgeFactor" type="number" step=".05" min=".1" value="1"></div><div class="input"><label>Availability</label><select name="closed"><option value="false">Open for booking</option><option value="true">Closed</option></select></div></div><div class="form-actions"><button class="btn btn-dark">Update date range</button></div></form><br><div class="inventory-table"><table><thead><tr><th>Date</th><th>Booked</th><th>Capacity</th><th>Price</th><th>Multiplier</th><th>State</th></tr></thead><tbody>${items.slice(0,60).map(x=>`<tr><td>${formatDate(x.date)}</td><td>${x.bookedCount}</td><td>${x.totalCount}</td><td>${money(x.price)}</td><td>${x.surgeFactor}×</td><td><span class="status ${x.closed?"cancelled":"active"}">${x.closed?"Closed":"Open"}</span></td></tr>`).join("")}</tbody></table></div>`;}catch(e){document.querySelector("#inventory-content").innerHTML=errorState(e.message);}}
async function updateInventory(e,roomId,type){e.preventDefault();const d=Object.fromEntries(new FormData(e.target));d.surgeFactor=Number(d.surgeFactor);d.closed=d.closed==="true";try{await api(`/admin/inventory/rooms/${roomId}`,{method:"PATCH",body:JSON.stringify(d)});toast("Inventory updated");openInventory(roomId,type);}catch(err){toast(err.message,"error");}}

function requireAuth(){openAuth("login");navigate("/");}
function requireManager(){toast("A host account is required","error");navigate("/");openAuth("manager");}
function paymentPage(success){const params=new URLSearchParams(route().split("?")[1]);return layout(`<main class="page"><div class="container"><div class="empty"><div class="empty-icon">${success?"✓":"!"}</div><h1>${success?"Your stay is in motion":"Payment wasn’t completed"}</h1><p class="muted">${success?"We’re confirming your payment now. Your trip will update as soon as the confirmation arrives.":"Nothing has been charged. Your reserved room may remain held briefly."}</p><button class="btn btn-dark" onclick="navigate('/trips')">${success?"View my trips":"Return to trips"}</button></div></div></main>`);}

async function render() {
  const path=route().split("?")[0];
  if(path==="/auth/callback"){
    const token=new URLSearchParams(route().split("?")[1]).get("accessToken");
    if(token){setToken(token);history.replaceState(null,"",`${location.pathname}#/`);await loadUser();toast("Signed in with Google");navigate("/");return;}
  }
  if(!state.user&&state.token)await loadUser();
  if(path==="/"){document.querySelector("#app").innerHTML=homePage();loadFeaturedStays();return;}
  if(path==="/search"){await searchPage();return;}
  if(path.startsWith("/hotel/")){await hotelPage(path.split("/")[2]);return;}
  if(path==="/trips"){await tripsPage();return;}
  if(path==="/profile"){await profilePage();return;}
  if(path==="/admin"){await adminPage();return;}
  if(path.startsWith("/admin/hotel/")){await adminHotelPage(path.split("/")[3]);return;}
  if(path==="/payments/success"){document.querySelector("#app").innerHTML=paymentPage(true);return;}
  if(path==="/payments/failure"){document.querySelector("#app").innerHTML=paymentPage(false);return;}
  document.querySelector("#app").innerHTML=layout(`<main class="page"><div class="container">${errorState("That page wandered off.")}</div></main>`);
}

window.addEventListener("hashchange",render);
window.addEventListener("DOMContentLoaded",render);
