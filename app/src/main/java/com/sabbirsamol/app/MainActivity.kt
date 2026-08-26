<!DOCTYPE html>
<html lang="bn">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Sabbir's Amol - ইসলামিক অ্যাপ</title>
  <style>
    :root {
      --bg-color: #f4f6f9;
      --card-bg: #ffffff;
      --text-main: #222222;
      --text-sub: #555555;
      --primary: #d97706;
      --primary-light: #fffdf5;
      --border-color: #e5e7eb;
      --nav-bg: #ffffff;
    }

    body.dark-mode {
      --bg-color: #121824;
      --card-bg: #1e293b;
      --text-main: #f8fafc;
      --text-sub: #94a3b8;
      --primary: #f59e0b;
      --primary-light: #1e293b;
      --border-color: #334155;
      --nav-bg: #0f172a;
    }

    * {
      box-sizing: border-box;
      margin: 0;
      padding: 0;
      font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
    }

    body {
      background-color: var(--bg-color);
      display: flex;
      justify-content: center;
      padding: 10px;
    }

    .app-container {
      width: 100%;
      max-width: 440px;
      background: var(--card-bg);
      border-radius: 16px;
      box-shadow: 0 4px 15px rgba(0,0,0,0.06);
      padding-bottom: 80px;
      min-height: 100vh;
      position: relative;
    }

    .top-action-bar {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 12px;
      margin: 10px 12px 0 12px;
      background: var(--primary-light);
      border: 1px solid var(--border-color);
      border-radius: 12px;
    }

    .theme-btn {
      background: none;
      border: 1px solid var(--border-color);
      padding: 6px 12px;
      border-radius: 20px;
      cursor: pointer;
      font-size: 13px;
      color: var(--text-main);
    }

    .top-time-bar {
      border: 1.5px solid #6ba4ff;
      background: rgba(107, 164, 255, 0.1);
      border-radius: 12px;
      margin: 10px 12px;
      padding: 10px;
      text-align: center;
      color: #2563eb;
      font-weight: bold;
      font-size: 16px;
    }
    body.dark-mode .top-time-bar { color: #60a5fa; border-color: #3b82f6; }

    .card {
      background: var(--card-bg);
      border: 1px solid var(--border-color);
      border-radius: 12px;
      margin: 12px;
      padding: 14px;
    }

    .status-card {
      background: var(--primary-light);
      border: 1.5px solid #d4a373;
      text-align: center;
    }
    .current-prayer-badge {
      font-size: 30px;
      font-weight: 800;
      color: var(--primary);
      margin: 6px 0;
    }

    .prayer-row {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 9px 0;
      border-bottom: 1px dashed var(--border-color);
      font-size: 15px;
      color: var(--text-main);
    }
    .prayer-time { color: #059669; font-weight: bold; }

    .tab-content { display: none; }
    .tab-content.active { display: block; }

    /* সূরা ডিজাইন */
    .surah-card {
      background: var(--bg-color);
      border: 1px solid var(--border-color);
      border-radius: 8px;
      margin-bottom: 10px;
      overflow: hidden;
    }
    .surah-header {
      padding: 12px;
      font-weight: bold;
      color: var(--text-main);
      display: flex;
      justify-content: space-between;
      cursor: pointer;
      background: var(--primary-light);
    }
    .surah-body {
      padding: 12px;
      font-size: 13.5px;
      line-height: 1.6;
      color: var(--text-sub);
      display: none;
      border-top: 1px solid var(--border-color);
    }

    /* ক্যালেন্ডার গ্রিড */
    .cal-controls {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 10px;
    }
    .cal-grid {
      display: grid;
      grid-template-columns: repeat(7, 1fr);
      gap: 4px;
      text-align: center;
    }
    .cal-head { font-size: 12px; font-weight: bold; color: var(--text-sub); padding: 4px; }
    .cal-day {
      padding: 8px 2px;
      font-size: 13px;
      border: 1px solid var(--border-color);
      border-radius: 6px;
      background: var(--bg-color);
      color: var(--text-main);
    }
    .cal-day.today { background: var(--primary); color: #fff; font-weight: bold; }

    /* কাজা ট্র্যাকার */
    .qaza-grid {
      display: grid;
      grid-template-columns: repeat(5, 1fr);
      gap: 5px;
      margin-top: 8px;
    }
    .qaza-box {
      border: 1px solid var(--border-color);
      border-radius: 6px;
      padding: 6px 2px;
      text-align: center;
      background: var(--bg-color);
    }
    .qaza-num { font-size: 15px; font-weight: bold; color: var(--primary); margin: 3px 0; }
    .q-btn { background: var(--card-bg); border: 1px solid var(--border-color); color: var(--text-main); width: 22px; height: 22px; border-radius: 3px; cursor: pointer; }

    /* বটম নেভিগেশন */
    .bottom-nav {
      position: fixed;
      bottom: 0;
      width: 100%;
      max-width: 440px;
      background: var(--nav-bg);
      border-top: 1px solid var(--border-color);
      display: flex;
      justify-content: space-around;
      padding: 8px 0;
      z-index: 100;
    }
    .nav-item {
      text-align: center;
      font-size: 11px;
      color: var(--text-sub);
      text-decoration: none;
      cursor: pointer;
    }
    .nav-item.active { color: var(--primary); font-weight: bold; }
    .nav-item span { display: block; font-size: 18px; margin-bottom: 2px; }
  </style>
</head>
<body>

<div class="app-container">

  <!-- টপ বার -->
  <div class="top-action-bar">
    <div style="font-size: 13px; font-weight: 600; color: var(--text-main);">📍 ঈশ্বরীপুর, সাতক্ষীরা</div>
    <button class="theme-btn" onclick="toggleDarkMode()">
      <span id="theme-icon">🌙</span> <span id="theme-text">ডার্ক মোড</span>
    </button>
  </div>

  <!-- লাইভ ঘড়ি -->
  <div class="top-time-bar">
    🕒 বর্তমান সময়: <span id="header-live-time">০০:০০:০০</span>
  </div>

  <!-- হোম ট্যাব -->
  <div id="tab-home" class="tab-content active">
    <div class="card status-card">
      <div style="font-size: 13px; color: var(--text-sub); font-weight: 600;">চলমান ওয়াক্ত</div>
      <div class="current-prayer-badge" id="active-prayer-name">যোহর</div>
    </div>

    <div class="card">
      <div style="font-weight: bold; color: var(--primary); font-size: 15px; margin-bottom: 8px;">🕌 ৫ ওয়াক্ত নামাজ (হানাফী)</div>
      <div class="prayer-row"><span>ফজর</span><span class="prayer-time">০৪:২৯ মি.</span></div>
      <div class="prayer-row"><span>যোহর</span><span class="prayer-time">১২:১২ মি.</span></div>
      <div class="prayer-row"><span>আসর</span><span class="prayer-time">০৪:৪১ মি.</span></div>
      <div class="prayer-row"><span>মাগরিব</span><span class="prayer-time">০৬:৩১ মি.</span></div>
      <div class="prayer-row"><span>এশা</span><span class="prayer-time">০৭:৪৮ মি.</span></div>
    </div>

    <div class="card">
      <div style="font-weight: bold; font-size: 15px; color: var(--primary); margin-bottom: 6px;">📋 কাজ়া নামাজ ট্র্যাকার</div>
      <div class="qaza-grid">
        <div class="qaza-box"><div style="font-size:11px;">ফজর</div><div class="qaza-num" id="qz-fajr">০</div><button class="q-btn" onclick="chQz('fajr',1)">+</button><button class="q-btn" onclick="chQz('fajr',-1)">-</button></div>
        <div class="qaza-box"><div style="font-size:11px;">যোহর</div><div class="qaza-num" id="qz-dhuhr">০</div><button class="q-btn" onclick="chQz('dhuhr',1)">+</button><button class="q-btn" onclick="chQz('dhuhr',-1)">-</button></div>
        <div class="qaza-box"><div style="font-size:11px;">আসর</div><div class="qaza-num" id="qz-asr">০</div><button class="q-btn" onclick="chQz('asr',1)">+</button><button class="q-btn" onclick="chQz('asr',-1)">-</button></div>
        <div class="qaza-box"><div style="font-size:11px;">মাগরিব</div><div class="qaza-num" id="qz-maghrib">০</div><button class="q-btn" onclick="chQz('maghrib',1)">+</button><button class="q-btn" onclick="chQz('maghrib',-1)">-</button></div>
        <div class="qaza-box"><div style="font-size:11px;">এশা</div><div class="qaza-num" id="qz-isha">০</div><button class="q-btn" onclick="chQz('isha',1)">+</button><button class="q-btn" onclick="chQz('isha',-1)">-</button></div>
      </div>
    </div>
  </div>

  <!-- সূরা ট্যাব (বিশেষ সূরাসমূহ) -->
  <div id="tab-surah" class="tab-content">
    <div class="card">
      <div style="font-weight: bold; color: var(--primary); font-size: 16px; margin-bottom: 12px;">📖 বিশেষ ফযিলতপূর্ণ সূরাসমূহ</div>

      <div class="surah-card">
        <div class="surah-header" onclick="toggleSurah('s-fatiha')"><span>সূরা আল-ফাতিহা</span><span>▼</span></div>
        <div id="s-fatiha" class="surah-body">বিসমিল্লাহির রাহমানির রাহীম। সমস্ত প্রশংসা আল্লাহ তাআলার জন্য, যিনি সমগ্র সৃষ্টির পালনকর্তা। পরম করুণাময়, অসীম দয়ালু। বিচার দিবসের মালিক...</div>
      </div>

      <div class="surah-card">
        <div class="surah-header" onclick="toggleSurah('s-baqarah')"><span>সূরা আল-বাকারা (নির্বাচিত আয়াত)</span><span>▼</span></div>
        <div id="s-baqarah" class="surah-body"><b>আয়াতুল কুরসি:</b> আল্লাহ, যিনি ব্যতীত কোনো উপাস্য নেই; যিনি চিরঞ্জীব, সর্বসত্তার ধারক। তাঁকে তন্দ্রাও স্পর্শ করতে পারে না, নিদ্রাও নয়...</div>
      </div>

      <div class="surah-card">
        <div class="surah-header" onclick="toggleSurah('s-yasin')"><span>সূরা ইয়াসিন</span><span>▼</span></div>
        <div id="s-yasin" class="surah-body">কুরআনের হৃৎপিণ্ড। ইয়াসীন। শপথ প্রজ্ঞাময় কুরআনের; নিশ্চয় আপনি রাসূলগণের অন্তর্ভুক্ত... প্রতিদিন সকালে পাঠে সারাদিনের কাজের সহজতা ও বরকত মেলে।</div>
      </div>

      <div class="surah-card">
        <div class="surah-header" onclick="toggleSurah('s-saffat')"><span>সূরা আস-সাফফাত</span><span>▼</span></div>
        <div id="s-saffat" class="surah-body">শপথ তাদের, যারা সারিবদ্ধ হয়ে দাঁড়ায়। শপথ তাদের, যারা ধমকিয়ে পরিচালনা করে... তাওহীদ ও ফেরেশতাদের আনুগত্যের বর্ণনামূলক বরকতময় সূরা।</div>
      </div>

      <div class="surah-card">
        <div class="surah-header" onclick="toggleSurah('s-rahman')"><span>সূরা আর-রহমান</span><span>▼</span></div>
        <div id="s-rahman" class="surah-body">পরম করুণাময় আল্লাহ; তিনিই কুরআন শিক্ষা দিয়েছেন, তিনিই মানুষ সৃষ্টি করেছেন... "অতএব, তোমরা তোমাদের রবের কোন কোন নিয়ামতকে অস্বীকার করবে?"</div>
      </div>

      <div class="surah-card">
        <div class="surah-header" onclick="toggleSurah('s-waqiah')"><span>সূরা আল-ওয়াকিয়া</span><span>▼</span></div>
        <div id="s-waqiah" class="surah-body">যখন সংঘটিত হবে মহাপ্রলয়... অভাব ও দারিদ্র্য দূরীকরণে প্রতি রাতে এ সূরা পাঠ করার বিশেষ উৎসাহ রয়েছে।</div>
      </div>
    </div>
  </div>

  <!-- ক্যালেন্ডার ট্যাব -->
  <div id="tab-cal" class="tab-content">
    <div class="card">
      <div class="cal-controls">
        <button class="q-btn" onclick="changeMonth(-1)">◀</button>
        <div id="cal-month-year" style="font-weight: bold; color: var(--primary); font-size: 15px;">মাস ও বছর</div>
        <button class="q-btn" onclick="changeMonth(1)">▶</button>
      </div>
      <div class="cal-grid">
        <div class="cal-head">রবি</div><div class="cal-head">সোম</div><div class="cal-head">মঙ্গল</div><div class="cal-head">বুধ</div><div class="cal-head">বৃহঃ</div><div class="cal-head">শুক্র</div><div class="cal-head">শনি</div>
      </div>
      <div id="auto-calendar-days" class="cal-grid"></div>
    </div>
  </div>

  <!-- কিবলা ট্যাব -->
  <div id="tab-qibla" class="tab-content">
    <div class="card" style="text-align: center; padding: 30px;">
      <div style="font-size: 44px; margin-bottom: 10px;">🧭</div>
      <div style="font-weight: bold; color: var(--primary); font-size: 16px;">কিবলার দিক</div>
      <div style="font-size: 13px; color: var(--text-sub); margin-top: 8px;">আপনার অবস্থান থেকে কাবা শরীফ পশ্চিম-উত্তর পশ্চিম (আনুমানিক ২৯৪°) কোণে অবস্থিত।</div>
    </div>
  </div>

  <!-- বটম মেনু -->
  <div class="bottom-nav">
    <a class="nav-item active" onclick="switchTab('home', this)"><span>🏠</span>হোম</a>
    <a class="nav-item" onclick="switchTab('surah', this)"><span>📖</span>সূরা</a>
    <a class="nav-item" onclick="switchTab('cal', this)"><span>📅</span>ক্যালেন্ডার</a>
    <a class="nav-item" onclick="switchTab('qibla', this)"><span>🧭</span>কিবলা</a>
  </div>

</div>

<script>
  function toBengali(num) {
    const b = ['০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯'];
    return num.toString().replace(/[0-9]/g, w => b[w]);
  }

  function switchTab(tabName, element) {
    document.querySelectorAll('.tab-content').forEach(el => el.classList.remove('active'));
    document.querySelectorAll('.nav-item').forEach(el => el.classList.remove('active'));
    document.getElementById('tab-' + tabName).classList.add('active');
    element.classList.add('active');
  }

  function toggleSurah(id) {
    const el = document.getElementById(id);
    el.style.display = el.style.display === 'block' ? 'none' : 'block';
  }

  // লাইভ ঘড়ি
  const timings = { fajr: "04:29", dhuhr: "12:12", asr: "16:41", maghrib: "18:31", isha: "19:48" };
  function parseTime(t) { const [h, m] = t.split(':').map(Number); const d = new Date(); d.setHours(h, m, 0, 0); return d; }

  function updateClock() {
    const now = new Date();
    let h = now.getHours();
    const m = String(now.getMinutes()).padStart(2, '0');
    const s = String(now.getSeconds()).padStart(2, '0');
    const ampm = h >= 12 ? 'অপরাহ্ন' : 'পূর্বাহ্ন';
    h = h % 12 || 12;

    document.getElementById("header-live-time").innerText = `${toBengali(String(h).padStart(2,'0'))}:${toBengali(m)}:${toBengali(s)} ${ampm}`;

    const f = parseTime(timings.fajr), d = parseTime(timings.dhuhr), a = parseTime(timings.asr), mg = parseTime(timings.maghrib), i = parseTime(timings.isha);
    let cur = "এশা";
    if (now >= f && now < d) cur = "ফজর";
    else if (now >= d && now < a) cur = "যোহর";
    else if (now >= a && now < mg) cur = "আসর";
    else if (now >= mg && now < i) cur = "মাগরিব";
    document.getElementById("active-prayer-name").innerText = cur;
  }
  setInterval(updateClock, 1000);
  updateClock();

  // ডায়নামিক ক্যালেন্ডার
  let calDate = new Date();
  function generateCalendar() {
    const year = calDate.getFullYear();
    const month = calDate.getMonth();
    const today = new Date();

    const monthNames = ["জানুয়ারি", "ফেব্রুয়ারি", "মার্চ", "এপ্রিল", "মে", "জুন", "জুলাই", "আগস্ট", "সেপ্টেম্বর", "অক্টোবর", "নভেম্বর", "ডিসেম্বর"];
    document.getElementById("cal-month-year").innerText = `${monthNames[month]} ${toBengali(year)}`;

    const firstDay = new Date(year, month, 1).getDay();
    const totalDays = new Date(year, month + 1, 0).getDate();
    const container = document.getElementById('auto-calendar-days');
    container.innerHTML = '';

    for (let i = 0; i < firstDay; i++) {
      container.appendChild(document.createElement('div'));
    }

    for (let d = 1; d <= totalDays; d++) {
      const cell = document.createElement('div');
      const isToday = (today.getFullYear() === year && today.getMonth() === month && today.getDate() === d);
      cell.className = 'cal-day' + (isToday ? ' today' : '');
      cell.innerText = toBengali(d);
      container.appendChild(cell);
    }
  }
  function changeMonth(offset) {
    calDate.setMonth(calDate.getMonth() + offset);
    generateCalendar();
  }
  generateCalendar();

  // কাজা ট্র্যাকার
  const qz = JSON.parse(localStorage.getItem('qz_data')) || { fajr: 0, dhuhr: 0, asr: 0, maghrib: 0, isha: 0 };
  function updateQzUI() {
    for (let k in qz) document.getElementById(`qz-${k}`).innerText = toBengali(qz[k]);
    localStorage.setItem('qz_data', JSON.stringify(qz));
  }
  function chQz(w, val) { qz[w] = Math.max(0, qz[w] + val); updateQzUI(); }
  updateQzUI();

  // ডার্ক মোড
  function toggleDarkMode() {
    document.body.classList.toggle('dark-mode');
    const isDark = document.body.classList.contains('dark-mode');
    document.getElementById('theme-icon').innerText = isDark ? '☀️' : '🌙';
    document.getElementById('theme-text').innerText = isDark ? 'লাইট মোড' : 'ডার্ক মোড';
    localStorage.setItem('theme', isDark ? 'dark' : 'light');
  }
  if (localStorage.getItem('theme') === 'dark') toggleDarkMode();
</script>

</body>
</html>
