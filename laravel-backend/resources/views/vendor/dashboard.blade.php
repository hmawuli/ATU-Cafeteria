<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Vendor Dashboard | ATU Cafeteria Console</title>
    <!-- PWA configuration and touch tags -->
    <link rel="manifest" href="/api/manifest.json">
    <meta name="theme-color" content="#4f46e5">
    <meta name="apple-mobile-web-app-capable" content="yes">
    <meta name="apple-mobile-web-app-status-bar-style" content="black-translucent">
    <link rel="apple-touch-icon" href="https://img.icons8.com/color/512/hamburger.png">
    
    <!-- Tailwind CSS CDN -->
    <script src="https://cdn.tailwindcss.com"></script>
    <link href="https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@300;400;500;600;700;800&family=JetBrains+Mono:wght@400;500;600&display=swap" rel="stylesheet">
    <!-- Chart.js CDN -->
    <script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
    <!-- QRCode.js Library CDN -->
    <script src="https://cdnjs.cloudflare.com/ajax/libs/qrcodejs/1.0.0/qrcode.min.js"></script>
    
    <style>
        body {
            font-family: 'Plus Jakarta Sans', sans-serif;
        }
        .code-font {
            font-family: 'JetBrains Mono', monospace;
        }
    </style>
</head>
<body class="bg-slate-50 min-h-screen text-slate-800">

    <!-- Top Banner Navigation -->
    <header class="bg-gradient-to-r from-indigo-900 via-indigo-800 to-slate-900 text-white shadow-xl">
        <div class="max-w-7xl mx-auto px-4 py-5 sm:px-6 lg:px-8 flex flex-col md:flex-row items-center justify-between gap-4">
            <div class="flex items-center gap-3">
                <div class="bg-indigo-600 text-white font-bold p-2.5 rounded-xl shadow-lg border border-indigo-400">
                    <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10"></path>
                    </svg>
                </div>
                <div>
                    <h1 class="text-xl md:text-2xl font-extrabold tracking-tight">Accra Technical University</h1>
                    <p class="text-xs text-indigo-300 font-semibold tracking-wider uppercase">Vendor Management Portal</p>
                </div>
            </div>

            <!-- Profile Overview Quick Action -->
            <div class="flex items-center gap-4">
                <div class="text-right">
                    <p class="text-sm font-medium text-slate-300">Logged in as:</p>
                    <p class="font-bold text-emerald-400 text-base">{{ $vendor->fullName }}</p>
                </div>
                <div class="h-10 w-10 rounded-full bg-indigo-500 border-2 border-indigo-300 flex items-center justify-center font-bold text-white shadow">
                    {{ substr($vendor->fullName, 0, 2) }}
                </div>
            </div>
        </div>
    </header>

    <main class="max-w-7xl mx-auto px-4 py-8 sm:px-6 lg:px-8">

        <!-- Vendor Quick Swap Controls (Outstanding developer touch!) -->
        <div class="bg-white rounded-2xl shadow-sm border border-slate-200 p-5 mb-8 flex flex-col md:flex-row items-center justify-between gap-4">
            <div>
                <h2 class="text-base font-bold text-slate-900">⚡ Developer Quick Profile Switcher</h2>
                <p class="text-xs text-slate-500 mt-1">Swap between seeded ATU cafeteria vendors in real-time to inspect their distinct metrics and catalogs.</p>
            </div>
            <div class="flex flex-wrap gap-2">
                <a href="?vendor_id=10" class="px-4 py-2 text-xs font-bold rounded-xl border transition-all duration-200 {{ $vendor->id == 10 ? 'bg-indigo-600 text-white border-indigo-600 shadow-md' : 'bg-slate-50 text-slate-700 hover:bg-slate-100 border-slate-200' }}">
                    👩‍🍳 Auntie Mary Joint (ID: 10)
                </a>
                <a href="?vendor_id=11" class="px-4 py-2 text-xs font-bold rounded-xl border transition-all duration-200 {{ $vendor->id == 11 ? 'bg-indigo-600 text-white border-indigo-600 shadow-md' : 'bg-slate-50 text-slate-700 hover:bg-slate-100 border-slate-200' }}">
                    👨‍🍳 Kofi Local Kitchen (ID: 11)
                </a>
                <a href="?vendor_id=12" class="px-4 py-2 text-xs font-bold rounded-xl border transition-all duration-200 {{ $vendor->id == 12 ? 'bg-indigo-600 text-white border-indigo-600 shadow-md' : 'bg-slate-50 text-slate-700 hover:bg-slate-100 border-slate-200' }}">
                    🍩 Bakery & Treats (ID: 12)
                </a>
            </div>
        </div>

        <!-- Feedback & Alerts Section -->
        @if(session('success'))
            <div class="bg-emerald-50 border-l-4 border-emerald-500 text-emerald-800 p-4 rounded-r-xl shadow-sm mb-8 flex items-start gap-3 transition-all duration-300">
                <svg class="w-5 h-5 text-emerald-500 mt-0.5 flex-shrink-0" fill="currentColor" viewBox="0 0 20 20">
                    <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clip-rule="evenodd"></path>
                </svg>
                <div>
                    <p class="font-bold">Operation Successful!</p>
                    <p class="text-sm opacity-90">{{ session('success') }}</p>
                </div>
            </div>
        @endif

        @if($errors->any())
            <div class="bg-rose-50 border-l-4 border-rose-500 text-rose-800 p-4 rounded-r-xl shadow-sm mb-8">
                <div class="flex items-start gap-3">
                    <svg class="w-5 h-5 text-rose-500 mt-0.5 flex-shrink-0" fill="currentColor" viewBox="0 0 20 20">
                        <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z" clip-rule="evenodd"></path>
                    </svg>
                    <div>
                        <p class="font-bold text-rose-900">Input Validation Failure</p>
                        <p class="text-sm opacity-95">Please correct the highlighted form errors to save updates:</p>
                    </div>
                </div>
                <ul class="list-disc list-inside mt-3 text-sm space-y-1 pl-8 text-rose-700 font-medium">
                    @foreach($errors->all() as $error)
                        <li>{{ $error }}</li>
                    @endforeach
                </ul>
            </div>
        @endif


        <!-- Overview Summary & Information Info -->
        <div class="grid grid-cols-1 lg:grid-cols-3 gap-8 mb-8">
            <div class="lg:col-span-2 bg-gradient-to-br from-slate-900 via-slate-800 to-indigo-950 text-white rounded-3xl p-6 shadow-xl flex flex-col justify-between relative overflow-hidden">
                <!-- Background decoration pattern -->
                <div class="absolute right-0 bottom-0 opacity-10 transform translate-x-12 translate-y-12">
                    <svg class="w-80 h-80" fill="currentColor" viewBox="0 0 20 20">
                        <path d="M10.894 2.553a1 1 0 00-1.788 0l-7 14a1 1 0 001.169 1.409l5-1.429A1 1 0 009 15.571V11a1 1 0 112 0v4.571a1 1 0 00.725.962l5 1.428a1 1 0 001.17-1.408l-7-14z"></path>
                    </svg>
                </div>

                <div class="z-10">
                    <div class="flex items-center justify-between mb-4">
                        <span class="px-3.5 py-1.5 bg-emerald-500/20 text-emerald-300 font-bold text-xs tracking-wider rounded-full uppercase border border-emerald-500/30">
                            {{ $vendor->is_open ? '● Open for Business' : '○ Closed / Offline' }}
                        </span>
                        <span class="text-xs text-slate-400 font-medium code-font">Registered: {{ date('F Y', strtotime($vendor->created_at)) }}</span>
                    </div>
                    <h3 class="text-3xl font-extrabold text-slate-100">{{ $vendor->profile_info['outlet_name'] ?? $vendor->fullName }}</h3>
                    <p class="text-slate-300 mt-2 text-sm leading-relaxed max-w-xl">
                        Operating under secure ID <span class="code-font text-emerald-400 font-semibold">{{ $vendor->student_staff_id }}</span>. Offering high-quality, authentic cafeteria catering to the Accra Technical University community. Manage inventory and view performance audits below.
                    </p>
                </div>

                <div class="z-10 grid grid-cols-2 md:grid-cols-3 gap-4 pt-6 mt-6 border-t border-slate-700/50">
                    <div>
                        <p class="text-xs text-slate-400 font-semibold tracking-wider uppercase">Campus Hub</p>
                        <p class="text-sm font-bold text-slate-200 mt-0.5">{{ $vendor->profile_info['location'] ?? 'ATU Cafeteria Block' }}</p>
                    </div>
                    <div>
                        <p class="text-xs text-slate-400 font-semibold tracking-wider uppercase">Support Tel</p>
                        <p class="text-sm font-bold text-slate-200 mt-0.5">{{ $vendor->profile_info['telephone'] ?? 'N/A' }}</p>
                    </div>
                    <div class="col-span-2 md:col-span-1">
                        <p class="text-xs text-slate-400 font-semibold tracking-wider uppercase">Core Category</p>
                        <p class="text-sm font-bold text-slate-200 mt-0.5">{{ $vendor->profile_info['primary_category'] ?? 'General Catering' }}</p>
                    </div>
                </div>
            </div>

            <!-- Virtual Balance Panel -->
            <div class="bg-white rounded-3xl p-6 shadow-sm border border-slate-200 flex flex-col justify-between">
                <div>
                    <h4 class="text-sm font-bold tracking-wider text-slate-400 uppercase">Virtual Wallet Ledger</h4>
                    <p class="text-xs text-slate-500 mt-1">Direct payout transactions are finalized in secure escrow.</p>
                </div>
                
                <div class="py-6">
                    <p class="text-xs text-slate-400 font-medium">Accumulated ESCROW Balance</p>
                    <div class="flex items-baseline gap-2 mt-1">
                        <span class="text-4xl font-black text-slate-900 code-font">GH₵ {{ number_format($vendor->balance, 2) }}</span>
                    </div>
                </div>

                <div class="bg-slate-50 rounded-2xl p-4 border border-slate-100 text-xs text-slate-600 flex items-center gap-3">
                    <svg class="w-5 h-5 text-indigo-500 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z"></path>
                    </svg>
                    <span>All customer pre-payments are cryptographically locked until order pick-up PIN verification.</span>
                </div>
            </div>
        </div>


        <!-- Performance Metrics Grid -->
        <h3 class="text-lg font-extrabold text-slate-900 mb-4 flex items-center gap-2">
            <svg class="w-5 h-5 text-indigo-600" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z"></path>
            </svg>
            Performance Metrics Summary
        </h3>
        <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6 mb-8">
            
            <!-- Metric 1: Average Completion Time -->
            <div class="bg-white rounded-2xl p-5 shadow-sm border border-slate-200 flex items-center gap-4 transition-all hover:translate-y-[-2px] hover:shadow-md">
                <div class="bg-indigo-50 text-indigo-600 p-3 rounded-xl">
                    <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z"></path>
                    </svg>
                </div>
                <div>
                    <p class="text-xs text-slate-400 font-bold uppercase tracking-wider">Avg prep time</p>
                    <p class="text-xl font-extrabold text-slate-900 mt-1 code-font">{{ $metrics['completion_time_metrics']['average_formatted'] }}</p>
                    <p class="text-[10px] text-slate-500 mt-0.5">Fastest: {{ $metrics['completion_time_metrics']['fastest_formatted'] }}</p>
                </div>
            </div>

            <!-- Metric 2: Completion Rate -->
            <div class="bg-white rounded-2xl p-5 shadow-sm border border-slate-200 flex items-center gap-4 transition-all hover:translate-y-[-2px] hover:shadow-md">
                <div class="bg-emerald-50 text-emerald-600 p-3 rounded-xl">
                    <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"></path>
                    </svg>
                </div>
                <div>
                    <p class="text-xs text-slate-400 font-bold uppercase tracking-wider">Completion Rate</p>
                    <p class="text-xl font-extrabold text-slate-900 mt-1 code-font">{{ $metrics['order_metrics']['completion_rate_percentage'] }}%</p>
                    <p class="text-[10px] text-emerald-600 font-semibold mt-0.5">High Performance</p>
                </div>
            </div>

            <!-- Metric 3: Total Completed Revenue -->
            <div class="bg-white rounded-2xl p-5 shadow-sm border border-slate-200 flex items-center gap-4 transition-all hover:translate-y-[-2px] hover:shadow-md">
                <div class="bg-amber-50 text-amber-600 p-3 rounded-xl">
                    <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8c-1.657 0-3 .895-3 2s1.343 2 3 2 3 .895 3 2-1.343 2-3 2m0-8c1.11 0 2.08.402 2.599 1M12 8V7m0 1v8m0 0v1m0-1c-1.11 0-2.08-.402-2.599-1M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path>
                    </svg>
                </div>
                <div>
                    <p class="text-xs text-slate-400 font-bold uppercase tracking-wider">Completed Sales</p>
                    <p class="text-xl font-extrabold text-slate-900 mt-1 code-font">GH₵ {{ number_format($metrics['order_metrics']['total_completed_revenue'], 2) }}</p>
                    <p class="text-[10px] text-slate-500 mt-0.5">Total: {{ $metrics['order_metrics']['total_orders_placed'] }} Orders</p>
                </div>
            </div>

            <!-- Metric 4: Overall Rating -->
            <div class="bg-white rounded-2xl p-5 shadow-sm border border-slate-200 flex items-center gap-4 transition-all hover:translate-y-[-2px] hover:shadow-md">
                <div class="bg-rose-50 text-rose-600 p-3 rounded-xl">
                    <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11.049 2.927c.3-.921 1.603-.921 1.902 0l1.519 4.674a1 1 0 00.95.69h4.915c.969 0 1.371 1.24.588 1.81l-3.976 2.888a1 1 0 00-.363 1.118l1.518 4.674c.3.922-.755 1.688-1.538 1.118l-3.976-2.888a1 1 0 00-1.176 0l-3.976 2.888c-.783.57-1.838-.197-1.538-1.118l1.518-4.674a1 1 0 00-.363-1.118l-3.976-2.888c-.784-.57-.38-1.81.588-1.81h4.914a1 1 0 00.951-.69l1.519-4.674z"></path>
                    </svg>
                </div>
                <div>
                    <p class="text-xs text-slate-400 font-bold uppercase tracking-wider">Quality Score</p>
                    <p class="text-xl font-extrabold text-slate-900 mt-1 code-font">{{ $metrics['rating_metrics']['overall_average_rating'] }} / 5.0</p>
                    <p class="text-[10px] text-slate-500 mt-0.5">Audited Feedbacks: {{ $metrics['rating_metrics']['total_feedback_count'] }}</p>
                </div>
            </div>

        </div>

        <!-- Interactive Controls Bar -->
        <div class="bg-white rounded-2xl shadow-sm border border-slate-200 p-4 mb-8 flex flex-col sm:flex-row items-center justify-between gap-4">
            <div class="flex items-center gap-2.5">
                <div class="bg-indigo-50 text-indigo-600 p-2 rounded-lg">
                    <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 6V4m0 2a2 2 0 100 4m0-4a2 2 0 110 4m-6 8a2 2 0 100-4m0 4a2 2 0 110-4m0 4v2m0-6V4m6 6v10m6-2a2 2 0 100-4m0 4a2 2 0 110-4m0 4v2m0-6V4"></path>
                    </svg>
                </div>
                <div>
                    <h3 class="text-sm font-extrabold text-slate-800">Dashboard Actions & Analysis Tools</h3>
                    <p class="text-[11px] text-slate-400">Download compiled analytics or visualize top performing items.</p>
                </div>
            </div>
            <div class="flex flex-wrap items-center gap-2">
                <!-- Top Performing Items Modal Trigger -->
                <button type="button" onclick="openTopItemsModal()" class="px-4 py-2 bg-indigo-50 hover:bg-indigo-100 text-indigo-700 text-xs font-bold rounded-xl transition-all duration-150 flex items-center gap-1.5 border border-indigo-200 shadow-sm">
                    <svg class="w-4 h-4 text-indigo-600" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4M7.835 4.697a3.42 3.42 0 001.946-.806 3.42 3.42 0 014.438 0 3.42 3.42 0 001.946.806 3.42 3.42 0 013.138 3.138 3.42 3.42 0 00.806 1.946 3.42 3.42 0 010 4.438 3.42 3.42 0 00-.806 1.946 3.42 3.42 0 01-3.138 3.138 3.42 3.42 0 00-1.946.806 3.42 3.42 0 01-4.438 0 3.42 3.42 0 00-1.946-.806 3.42 3.42 0 01-3.138-3.138 3.42 3.42 0 00-.806-1.946 3.42 3.42 0 010-4.438 3.42 3.42 0 00.806-1.946 3.42 3.42 0 013.138-3.138z"></path>
                    </svg>
                    🏆 Analyze Top Menu Items
                </button>

                <!-- Export CSV Button -->
                <a href="/api/vendor/export-csv?vendor_id={{ $vendor->id }}" class="px-4 py-2 bg-emerald-600 hover:bg-emerald-700 text-white text-xs font-bold rounded-xl transition-all duration-150 flex items-center gap-1.5 shadow-md">
                    <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 10v6m0 0l-3-3m3 3l3-3m2 8H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"></path>
                    </svg>
                    Export CSV Performance Report
                </a>
            </div>
        </div>


        <!-- Weekly Sales Chart Section -->
        <div class="bg-white rounded-3xl border border-slate-200 shadow-sm p-6 mb-8">
            <div class="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 mb-6">
                <div>
                    <h3 class="text-lg font-bold text-slate-900 flex items-center gap-2">
                        <svg class="w-5 h-5 text-indigo-600" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 12l3-3 3 3 4-4M8 21h8a2 2 0 002-2V5a2 2 0 00-2-2H8a2 2 0 00-2 2v14a2 2 0 002 2z"></path>
                        </svg>
                        Weekly Sales Revenue Analytics
                    </h3>
                    <p class="text-xs text-slate-500 mt-0.5">Real-time daily transaction volume and compiled sales over the past 7 days.</p>
                </div>
                <div class="px-3 py-1 bg-emerald-50 text-emerald-700 text-xs font-bold rounded-lg code-font">
                    Active Ledger
                </div>
            </div>
            
            <div class="relative w-full h-[280px]">
                <canvas id="weeklySalesChart"></canvas>
            </div>
        </div>


        <!-- Inventory & Form Section -->
        <div class="grid grid-cols-1 lg:grid-cols-3 gap-8">
            
            <!-- Left Side: Menu Catalog List Table & Orders -->
            <div class="lg:col-span-2 flex flex-col gap-6">

                <!-- Incoming Orders & Fulfillment Console -->
                <div class="bg-white rounded-3xl border border-slate-200 shadow-sm overflow-hidden mb-2">
                    <div class="px-6 py-5 border-b border-slate-100 flex flex-col md:flex-row md:items-center justify-between gap-4">
                        <div>
                            <h3 class="text-lg font-bold text-slate-900 flex items-center gap-2">
                                <span class="animate-pulse inline-block h-2.5 w-2.5 rounded-full bg-rose-500"></span>
                                📦 Incoming Orders & Fulfillment Console
                            </h3>
                            <p class="text-xs text-slate-500 mt-0.5">Real-time orders received from ATU students. Update their preparation status in the database below.</p>
                        </div>
                        <span class="px-3 py-1 bg-indigo-50 text-indigo-700 text-xs font-bold rounded-lg code-font">
                            {{ count($orders) }} Total Orders
                        </span>
                    </div>

                    <!-- Desktop-only view table -->
                    <div class="hidden md:block overflow-x-auto">
                        <table class="w-full text-left border-collapse">
                            <thead>
                                <tr class="bg-slate-50 border-b border-slate-200 text-[11px] text-slate-400 font-extrabold uppercase tracking-wider">
                                    <th class="py-4 px-6 text-center">Order ID</th>
                                    <th class="py-4 px-4">Student Name</th>
                                    <th class="py-4 px-4">Dish Details</th>
                                    <th class="py-4 px-4 text-center">Qty</th>
                                    <th class="py-4 px-4 text-right">Total Price</th>
                                    <th class="py-4 px-4">Date Placed</th>
                                    <th class="py-4 px-4 text-center">Fulfillment Status</th>
                                    <th class="py-4 px-6 text-center">Fulfill Action</th>
                                </tr>
                            </thead>
                            <tbody class="divide-y divide-slate-100 text-sm">
                                @forelse($orders as $order)
                                    <tr class="hover:bg-slate-50/50 transition-colors duration-150">
                                        <td class="py-4 px-6 text-center code-font text-xs font-semibold text-slate-400">
                                            #{{ $order->id }}
                                        </td>
                                        <td class="py-4 px-4 font-semibold text-slate-800">
                                            @if($order->customer)
                                                {{ $order->customer->fullName }}
                                            @elseif($order->student)
                                                {{ $order->student->fullName }}
                                            @else
                                                Student (ID: {{ $order->customer_id }})
                                            @endif
                                            <div class="text-[10px] text-indigo-500 code-font">PIN: {{ $order->pickup_pin }}</div>
                                        </td>
                                        <td class="py-4 px-4 font-bold text-slate-900">
                                            {{ $order->food_name ?? 'N/A' }}
                                        </td>
                                        <td class="py-4 px-4 text-center font-medium text-slate-600">
                                            {{ $order->quantity ?? 1 }}
                                        </td>
                                        <td class="py-4 px-4 text-right font-bold text-slate-900 code-font">
                                            GH₵ {{ number_format($order->total_price, 2) }}
                                        </td>
                                        <td class="py-4 px-4 text-xs text-slate-500">
                                            {{ date('M d, Y H:i', $order->order_timestamp / 1000) }}
                                        </td>
                                        <td class="py-4 px-4 text-center">
                                            @php
                                                $statusColors = [
                                                    'PENDING' => 'bg-amber-50 text-amber-700 border-amber-200',
                                                    'ORDER_PLACED' => 'bg-indigo-50 text-indigo-700 border-indigo-200',
                                                    'PREPARING' => 'bg-blue-50 text-blue-700 border-blue-200',
                                                    'READY' => 'bg-emerald-50 text-emerald-700 border-emerald-200',
                                                    'COMPLETED' => 'bg-slate-100 text-slate-705 border-slate-300',
                                                    'DECLINED' => 'bg-rose-50 text-rose-700 border-rose-200',
                                                    'CANCELLED' => 'bg-red-50 text-red-700 border-red-200',
                                                ];
                                                $currStatus = strtoupper($order->status);
                                                $badgeColor = $statusColors[$currStatus] ?? 'bg-slate-50 text-slate-600 border-slate-200';
                                            @endphp
                                            <span class="px-2.5 py-1 text-[10px] font-bold rounded-full border {{ $badgeColor }}">
                                                {{ $order->status }}
                                            </span>
                                        </td>
                                        <td class="py-4 px-6 text-center">
                                            <!-- Simple dropdown action form for state update -->
                                            <form action="/api/vendor/orders/{{ $order->id }}/update-status" method="POST" class="inline-flex items-center gap-1">
                                                @csrf
                                                <select name="status" onchange="this.form.submit()" class="px-2 py-1 text-xs rounded-lg border border-slate-300 bg-white text-slate-700 focus:outline-none focus:ring-1 focus:ring-indigo-500 transition-all cursor-pointer">
                                                    <option value="ORDER_PLACED" {{ $currStatus === 'ORDER_PLACED' ? 'selected' : '' }}>Pending</option>
                                                    <option value="PREPARING" {{ $currStatus === 'PREPARING' ? 'selected' : '' }}>Preparing</option>
                                                    <option value="READY" {{ $currStatus === 'READY' ? 'selected' : '' }}>Ready</option>
                                                    <option value="COMPLETED" {{ $currStatus === 'COMPLETED' ? 'selected' : '' }}>Completed</option>
                                                    <option value="DECLINED" {{ $currStatus === 'DECLINED' ? 'selected' : '' }}>Declined</option>
                                                </select>
                                            </form>
                                        </td>
                                    </tr>
                                @empty
                                    <tr>
                                        <td colspan="8" class="py-12 text-center">
                                            <div class="flex flex-col items-center justify-center text-slate-400">
                                                <svg class="w-12 h-12 stroke-current opacity-60" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M16 11V7a4 4 0 00-8 0v4M5 9h14l1 12H4L5 9z"></path>
                                                </svg>
                                                <p class="font-bold mt-2">No incoming orders found</p>
                                                <p class="text-xs mt-0.5">Orders placed by customers will be populated here.</p>
                                            </div>
                                        </td>
                                    </tr>
                                @endforelse
                            </tbody>
                        </table>
                    </div>

                    <!-- Mobile responsive card list for incoming orders -->
                    <div class="block md:hidden p-4 space-y-4">
                        @forelse($orders as $order)
                            <div class="bg-slate-50 rounded-2xl p-4 border border-slate-200 space-y-3 relative shadow-3xs">
                                <div class="flex items-center justify-between">
                                    <span class="text-xs font-semibold code-font text-slate-400">#{{ $order->id }}</span>
                                    @php
                                        $currStatus = strtoupper($order->status);
                                        $badgeColor = $statusColors[$currStatus] ?? 'bg-slate-50 text-slate-600 border-slate-200';
                                    @endphp
                                    <span class="px-2.5 py-1 text-[10px] font-bold rounded-full border {{ $badgeColor }}">
                                        {{ $order->status }}
                                    </span>
                                </div>
                                <div>
                                    <h4 class="font-extrabold text-slate-900 text-sm">{{ $order->food_name ?? 'N/A' }}</h4>
                                    <p class="text-xs text-slate-500 mt-0.5">
                                        By: 
                                        @if($order->customer)
                                            {{ $order->customer->fullName }}
                                        @elseif($order->student)
                                            {{ $order->student->fullName }}
                                        @else
                                            Student (ID: {{ $order->customer_id }})
                                        @endif
                                    </p>
                                    <div class="text-[10px] text-indigo-600 font-extrabold code-font mt-1 flex items-center gap-1">
                                        <span>🔑 Pickup PIN:</span>
                                        <span class="bg-indigo-50 px-1.5 py-0.5 rounded border border-indigo-100 text-indigo-700">{{ $order->pickup_pin }}</span>
                                    </div>
                                </div>
                                <div class="flex items-center justify-between border-t border-slate-100 pt-2.5 text-xs">
                                    <div>
                                        <span class="text-slate-400 font-medium">Qty:</span> <strong class="text-slate-700 font-bold">{{ $order->quantity ?? 1 }}</strong>
                                        <span class="mx-1.5 text-slate-300">|</span>
                                        <strong class="text-indigo-600 font-black code-font">GH₵ {{ number_format($order->total_price, 2) }}</strong>
                                    </div>
                                    <span class="text-[10px] text-slate-400 font-medium">{{ date('H:i', $order->order_timestamp / 1000) }}</span>
                                </div>
                                <div class="bg-white p-2.5 rounded-xl border border-slate-200/60 flex items-center justify-between gap-2 mt-2 shadow-3xs">
                                    <span class="text-[11px] font-bold text-slate-500">Action:</span>
                                    <form action="/api/vendor/orders/{{ $order->id }}/update-status" method="POST" class="flex-grow max-w-[160px]">
                                        @csrf
                                        <select name="status" onchange="this.form.submit()" class="w-full px-2 py-1.5 text-xs rounded-lg border border-slate-300 bg-slate-50 text-slate-700 font-bold focus:outline-none focus:ring-2 focus:ring-indigo-500 transition-all cursor-pointer">
                                            <option value="ORDER_PLACED" {{ $currStatus === 'ORDER_PLACED' ? 'selected' : '' }}>Pending</option>
                                            <option value="PREPARING" {{ $currStatus === 'PREPARING' ? 'selected' : '' }}>Preparing</option>
                                            <option value="READY" {{ $currStatus === 'READY' ? 'selected' : '' }}>Ready</option>
                                            <option value="COMPLETED" {{ $currStatus === 'COMPLETED' ? 'selected' : '' }}>Completed</option>
                                            <option value="DECLINED" {{ $currStatus === 'DECLINED' ? 'selected' : '' }}>Declined</option>
                                        </select>
                                    </form>
                                </div>
                            </div>
                        @empty
                            <div class="py-12 text-center">
                                <div class="flex flex-col items-center justify-center text-slate-400">
                                    <svg class="w-12 h-12 stroke-current opacity-60" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M16 11V7a4 4 0 00-8 0v4M5 9h14l1 12H4L5 9z"></path>
                                    </svg>
                                    <p class="font-bold mt-2">No incoming orders found</p>
                                    <p class="text-xs mt-0.5">Orders placed by customers will be populated here.</p>
                                </div>
                            </div>
                        @endforelse
                    </div>
                </div>

                <div class="bg-white rounded-3xl border border-slate-200 shadow-sm overflow-hidden">
                    <div class="px-6 py-5 border-b border-slate-100 flex flex-col md:flex-row md:items-center justify-between gap-4">
                        <div>
                            <h3 class="text-lg font-bold text-slate-900">🍽️ Active Menu Catalog</h3>
                            <p class="text-xs text-slate-500 mt-0.5">Manage details and availability of dishes served at your booth.</p>
                        </div>
                        <div class="flex flex-wrap items-center gap-3">
                            <!-- Elegant Search Bar Form -->
                            <form action="" method="GET" class="flex items-center gap-2">
                                <input type="hidden" name="vendor_id" value="{{ $vendor->id }}">
                                <div class="relative">
                                    <input type="text" name="search" value="{{ $search ?? '' }}" placeholder="Search by name..." class="pl-8 pr-3 py-1.5 text-xs rounded-xl border border-slate-300 focus:ring-2 focus:ring-indigo-500 outline-none w-44 transition-all">
                                    <svg class="w-3.5 h-3.5 text-slate-400 absolute left-2.5 top-2" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"></path>
                                    </svg>
                                </div>
                                <button type="submit" class="px-3 py-1.5 bg-indigo-600 hover:bg-indigo-700 text-white text-xs font-bold rounded-xl shadow transition-all">
                                    Search
                                </button>
                                @if(!empty($search))
                                    <a href="?vendor_id={{ $vendor->id }}" class="px-3 py-1.5 bg-slate-100 hover:bg-slate-200 text-slate-700 text-xs font-semibold rounded-xl transition-all">
                                        Clear
                                    </a>
                                @endif
                            </form>

                            <span class="px-3 py-1 bg-indigo-50 text-indigo-700 text-xs font-bold rounded-lg code-font">
                                {{ count($foodItems) }} Dishes Listed
                            </span>
                        </div>
                    </div>

                    <!-- Desktop-only view table -->
                    <div class="hidden md:block overflow-x-auto">
                        <table class="w-full text-left border-collapse">
                            <thead>
                                <tr class="bg-slate-50 border-b border-slate-200 text-[11px] text-slate-400 font-extrabold uppercase tracking-wider">
                                    <th class="py-4 px-6 text-center">ID</th>
                                    <th class="py-4 px-4">Dish Name</th>
                                    <th class="py-4 px-4">Category</th>
                                    <th class="py-4 px-4 text-right">Unit Price</th>
                                    <th class="py-4 px-4">Description</th>
                                    <th class="py-4 px-4 text-center">Status</th>
                                    <th class="py-4 px-6 text-center">Actions</th>
                                </tr>
                            </thead>
                            <tbody class="divide-y divide-slate-100 text-sm">
                                @forelse($foodItems as $food)
                                    <tr class="hover:bg-slate-50/50 transition-colors duration-150">
                                        <td class="py-4 px-6 text-center code-font text-xs font-semibold text-slate-400">{{ $food->id }}</td>
                                        <td class="py-4 px-4 font-bold text-slate-900">{{ $food->name }}</td>
                                        <td class="py-4 px-4">
                                            <span class="px-2 py-1 bg-slate-100 text-slate-700 text-[10px] font-bold rounded-md">
                                                {{ $food->category }}
                                            </span>
                                        </td>
                                        <td class="py-4 px-4 text-right font-bold text-indigo-600 code-font">
                                            GH₵ {{ number_format($food->price, 2) }}
                                        </td>
                                        <td class="py-4 px-4 text-xs text-slate-500 max-w-[200px] truncate" title="{{ $food->description }}">
                                            {{ $food->description }}
                                        </td>
                                        <td class="py-4 px-4 text-center">
                                            <!-- Simple status toggle action -->
                                            <form action="/api/vendor/food-items/{{ $food->id }}/toggle-status" method="POST" class="inline-block">
                                                @csrf
                                                <button type="submit" class="group focus:outline-none" title="Toggle stock status">
                                                    <span class="px-2.5 py-1 text-[10px] font-bold rounded-full border transition-all duration-150 cursor-pointer shadow-xs {{ $food->is_available ? 'bg-emerald-50 text-emerald-700 border-emerald-200 hover:bg-emerald-100' : 'bg-rose-50 text-rose-700 border-rose-200 hover:bg-rose-100' }}">
                                                        {{ $food->is_available ? 'In Stock ●' : 'Sold Out ○' }}
                                                    </span>
                                                </button>
                                            </form>
                                        </td>
                                        <td class="py-4 px-6 text-center">
                                            <div class="inline-flex items-center gap-1.5">
                                                <!-- Generate QR Code action button -->
                                                <button type="button" onclick="generateFoodItemQr('{{ $food->id }}', '{{ addslashes($food->name) }}', '{{ $food->price }}')" class="p-1.5 bg-indigo-50 text-indigo-600 hover:bg-indigo-100 hover:text-indigo-700 rounded-lg border border-indigo-200 transition-all shadow-sm flex items-center justify-center" title="Generate Customer Scan QR Code">
                                                    <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                                                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v1m6 11h2m-6 0h-2v4m0-11v3m0 0h.01M12 12h.01M16 12h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path>
                                                    </svg>
                                                </button>

                                                <!-- Web Delete Form Action -->
                                                <form action="/api/vendor/food-items/{{ $food->id }}/delete" method="POST" onsubmit="return confirm('Are you sure you want to remove \'{{ $food->name }}\' from your menu?')" class="inline-block">
                                                    @csrf
                                                    <button type="submit" class="p-1.5 bg-rose-50 text-rose-600 hover:bg-rose-100 hover:text-rose-700 rounded-lg border border-rose-200 transition-all shadow-sm" title="Delete Food Item">
                                                        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                                                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"></path>
                                                        </svg>
                                                    </button>
                                                </form>
                                            </div>
                                        </td>
                                    </tr>
                                @empty
                                    <tr>
                                        <td colspan="7" class="py-12 text-center">
                                            <div class="flex flex-col items-center justify-center text-slate-400">
                                                <svg class="w-12 h-12 stroke-current opacity-60" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 13h6m-3-3v6m-9 1V4a2 2 0 012-2h6l2 2h6a2 2 0 012 2v8a2 2 0 01-2 2H5a2 2 0 01-2-2z"></path>
                                                </svg>
                                                <p class="font-bold mt-2">No food items added yet</p>
                                                <p class="text-xs mt-0.5">Use the creation panel on the right to list your first dish.</p>
                                            </div>
                                        </td>
                                    </tr>
                                @endforelse
                            </tbody>
                        </table>
                    </div>

                    <!-- Mobile responsive card list for Active Menu Catalog -->
                    <div class="block md:hidden p-4 space-y-4">
                        @forelse($foodItems as $food)
                            <div class="bg-slate-50 rounded-2xl p-4 border border-slate-200 space-y-3 shadow-3xs">
                                <div class="flex items-center justify-between">
                                    <span class="px-2 py-1 bg-white border border-slate-200 text-slate-700 text-[10px] font-bold rounded-md">
                                        {{ $food->category }}
                                    </span>
                                    <strong class="text-indigo-600 font-extrabold text-sm code-font">GH₵ {{ number_format($food->price, 2) }}</strong>
                                </div>
                                <div>
                                    <h4 class="font-bold text-slate-900 text-sm">{{ $food->name }}</h4>
                                    <p class="text-xs text-slate-500 mt-1 leading-relaxed">{{ $food->description }}</p>
                                </div>
                                <div class="flex items-center justify-between border-t border-slate-200/50 pt-3 mt-1.5">
                                    <!-- Stock Status Toggle -->
                                    <form action="/api/vendor/food-items/{{ $food->id }}/toggle-status" method="POST">
                                        @csrf
                                        <button type="submit" class="focus:outline-none">
                                            <span class="px-2.5 py-1 text-[10px] font-bold rounded-full border transition-all duration-150 cursor-pointer shadow-3xs {{ $food->is_available ? 'bg-emerald-50 text-emerald-700 border-emerald-200' : 'bg-rose-50 text-rose-700 border-rose-200' }}">
                                                {{ $food->is_available ? 'In Stock ●' : 'Sold Out ○' }}
                                            </span>
                                        </button>
                                    </form>

                                    <!-- Touch actions buttons -->
                                    <div class="flex items-center gap-2">
                                        <!-- QR Code Button -->
                                        <button type="button" onclick="generateFoodItemQr('{{ $food->id }}', '{{ addslashes($food->name) }}', '{{ $food->price }}')" class="px-2.5 py-1.5 bg-indigo-50 text-indigo-600 border border-indigo-200 text-xs font-bold rounded-xl flex items-center gap-1 transition-all" style="min-height: 40px;">
                                            <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v1m6 11h2m-6 0h-2v4m0-11v3m0 0h.01M12 12h.01M16 12h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path>
                                            </svg>
                                            <span>QR Code</span>
                                        </button>

                                        <!-- Delete action -->
                                        <form action="/api/vendor/food-items/{{ $food->id }}/delete" method="POST" onsubmit="return confirm('Are you sure you want to remove \'{{ $food->name }}\' from your menu?')" class="inline-block">
                                            @csrf
                                            <button type="submit" class="p-2 bg-rose-50 text-rose-600 rounded-xl border border-rose-200 flex items-center justify-center transition-all" style="min-width: 40px; min-height: 40px;">
                                                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"></path>
                                                </svg>
                                            </button>
                                        </form>
                                    </div>
                                </div>
                            </div>
                        @empty
                            <div class="py-12 text-center">
                                <div class="flex flex-col items-center justify-center text-slate-400">
                                    <svg class="w-12 h-12 stroke-current opacity-60" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 13h6m-3-3v6m-9 1V4a2 2 0 012-2h6l2 2h6a2 2 0 012 2v8a2 2 0 01-2 2H5a2 2 0 01-2-2z"></path>
                                    </svg>
                                    <p class="font-bold mt-2">No food items added yet</p>
                                    <p class="text-xs mt-0.5">Use the creation panel on the right to list your first dish.</p>
                                </div>
                            </div>
                        @endforelse
                    </div>
                </div>

                <!-- Recent Audited Feedback logs -->
                <div class="bg-white rounded-3xl border border-slate-200 shadow-sm p-6">
                    <h3 class="text-lg font-bold text-slate-900 mb-4 flex items-center gap-2">
                        <span>💬 Recent Customer Feedback Log</span>
                    </h3>
                    <div class="space-y-4">
                        @forelse($metrics['recent_customer_feedback'] as $fb)
                            <div class="border-b border-slate-100 last:border-none pb-4 last:pb-0">
                                <div class="flex items-center justify-between gap-4">
                                    <div class="flex items-center gap-2">
                                        <div class="h-7 w-7 rounded-full bg-slate-100 font-semibold text-xs text-slate-700 flex items-center justify-center border border-slate-200">
                                            {{ substr($fb['customer_name'], 0, 1) }}
                                        </div>
                                        <p class="font-bold text-xs text-slate-800">{{ $fb['customer_name'] }}</p>
                                    </div>
                                    <div class="flex items-center gap-1.5">
                                        <!-- Quality star -->
                                        <span class="text-amber-400 text-xs font-bold flex items-center gap-0.5">
                                            ★ {{ $fb['ratings']['average'] }}
                                        </span>
                                        <span class="text-[10px] text-slate-400 code-font">{{ date('Y-m-d H:i', strtotime($fb['submitted_at'])) }}</span>
                                    </div>
                                </div>
                                <p class="text-xs text-slate-600 mt-2 italic pl-9 bg-slate-50/50 p-2.5 rounded-xl border border-slate-100/80">
                                    "{{ $fb['comment'] }}"
                                </p>
                            </div>
                        @empty
                            <p class="text-xs text-slate-400 italic text-center py-4">No reviews or ratings received for this vendor yet.</p>
                        @endforelse
                    </div>
                </div>

            </div>

            <!-- Right Side: Create Food Item Form (Validated!) -->
            <div>
                <div class="bg-white rounded-3xl border border-slate-200 shadow-sm p-6 sticky top-6">
                    <h3 class="text-lg font-bold text-slate-900 mb-4 flex items-center gap-2">
                        <svg class="w-5 h-5 text-indigo-600" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v3m0 0v3m0-3h3m-3 0H9m12 0a9 9 0 11-18 0 9 9 0 0118 0z"></path>
                        </svg>
                        Add Menu Food Item
                    </h3>
                    <p class="text-xs text-slate-500 mb-6">List a new culinary masterpiece on the student-facing catalog. Submits through custom validator.</p>

                    <form action="/api/vendor/food-items" method="POST" class="space-y-4">
                        @csrf
                        <!-- Hidden vendor ID reference -->
                        <input type="hidden" name="vendor_id" value="{{ $vendor->id }}">

                        <div>
                            <label class="block text-xs font-bold text-slate-600 uppercase tracking-wider mb-1">Dish Name</label>
                            <input type="text" name="name" value="{{ old('name') }}" placeholder="e.g. Accra Kelewele Cups" required class="w-full px-4 py-2.5 text-sm rounded-xl border border-slate-300 focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 outline-none transition-all placeholder:text-slate-400">
                        </div>

                        <div class="grid grid-cols-2 gap-4">
                            <div>
                                <label class="block text-xs font-bold text-slate-600 uppercase tracking-wider mb-1">Category</label>
                                <select name="category" required class="w-full px-3 py-2.5 text-sm rounded-xl border border-slate-300 focus:ring-2 focus:ring-indigo-500 outline-none transition-all bg-white text-slate-700">
                                    <option value="Lunch Specials" {{ old('category') == 'Lunch Specials' ? 'selected' : '' }}>Lunch Specials</option>
                                    <option value="Traditional" {{ old('category') == 'Traditional' ? 'selected' : '' }}>Traditional</option>
                                    <option value="Snacks" {{ old('category') == 'Snacks' ? 'selected' : '' }}>Snacks</option>
                                    <option value="Drinks" {{ old('category') == 'Drinks' ? 'selected' : '' }}>Drinks</option>
                                </select>
                            </div>
                            <div>
                                <label class="block text-xs font-bold text-slate-600 uppercase tracking-wider mb-1">Price (GH₵)</label>
                                <input type="number" step="0.01" name="price" value="{{ old('price') }}" placeholder="15.50" required class="w-full px-4 py-2.5 text-sm rounded-xl border border-slate-300 focus:ring-2 focus:ring-indigo-500 outline-none transition-all placeholder:text-slate-400 code-font">
                            </div>
                        </div>

                        <div>
                            <label class="block text-xs font-bold text-slate-600 uppercase tracking-wider mb-1">Dish Description</label>
                            <textarea name="description" rows="4" placeholder="Briefly describe preparation & ingredients (e.g. Spiced crispy fried plantain cubes, roasted groundnuts. Minimum 10 characters required)" required class="w-full px-4 py-2.5 text-sm rounded-xl border border-slate-300 focus:ring-2 focus:ring-indigo-500 outline-none transition-all placeholder:text-slate-400 leading-relaxed">{{ old('description') }}</textarea>
                            <p class="text-[10px] text-indigo-500 mt-1 font-semibold">★ Description must be at least 10 characters long to satisfy validation audits.</p>
                        </div>

                        <button type="submit" class="w-full py-3 bg-indigo-600 hover:bg-indigo-700 text-white font-bold rounded-xl transition-all shadow-md hover:shadow-lg flex items-center justify-center gap-2">
                            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"></path>
                            </svg>
                            Save and Publish Dish
                        </button>
                    </form>
                </div>
            </div>

        </div>

    </main>

    <footer class="bg-slate-900 text-slate-400 text-xs py-10 mt-16 border-t border-slate-800">
        <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 text-center space-y-2">
            <p class="font-bold text-slate-300">Accra Technical University Cafeteria Ecosystem Admin Panel</p>
            <p>Developed with absolute precision for high-performance PostgreSQL ACID transactional logs.</p>
            <p class="opacity-60">&copy; 2026 Accra Technical University. All rights reserved.</p>
        </div>
    </footer>

    <!-- Chart.js Initialization -->
    <script>
        document.addEventListener("DOMContentLoaded", function() {
            const ctx = document.getElementById('weeklySalesChart').getContext('2d');
            
            // Weekly sales data passed dynamically from Laravel
            const salesData = @json($weeklySales);
            
            const labels = salesData.map(item => item.day);
            const values = salesData.map(item => item.sales);
            
            new Chart(ctx, {
                type: 'line',
                data: {
                    labels: labels,
                    datasets: [{
                        label: 'Sales Revenue (GH₵)',
                        data: values,
                        borderColor: '#4f46e5', // indigo-600
                        backgroundColor: 'rgba(79, 70, 229, 0.06)',
                        borderWidth: 3,
                        pointBackgroundColor: '#4f46e5',
                        pointHoverBackgroundColor: '#10b981', // emerald-500
                        pointHoverRadius: 6,
                        tension: 0.3,
                        fill: true
                    }]
                },
                options: {
                    responsive: true,
                    maintainAspectRatio: false,
                    plugins: {
                        legend: {
                            display: false
                        },
                        tooltip: {
                            callbacks: {
                                label: function(context) {
                                    return ` Revenue: GH₵ ${context.parsed.y.toFixed(2)}`;
                                }
                            }
                        }
                    },
                    scales: {
                        y: {
                            beginAtZero: true,
                            grid: {
                                color: 'rgba(241, 245, 249, 1)'
                            },
                            ticks: {
                                callback: function(value) {
                                    return 'GH₵ ' + value;
                                },
                                font: {
                                    family: 'JetBrains Mono',
                                    size: 10
                                }
                            }
                        },
                        x: {
                            grid: {
                                display: false
                            },
                            ticks: {
                                font: {
                                    family: 'Plus Jakarta Sans',
                                    weight: '600',
                                    size: 11
                                }
                            }
                        }
                    }
                }
            });
        });
    </script>

    <!-- Interactive Top Performing Items Modal -->
    <div id="topItemsModal" class="fixed inset-0 z-50 overflow-y-auto hidden" aria-labelledby="modal-title" role="dialog" aria-modal="true">
        <div class="flex items-end justify-center min-h-screen pt-4 px-4 pb-20 text-center sm:block sm:p-0">
            <!-- Background overlay -->
            <div class="fixed inset-0 bg-slate-900 bg-opacity-75 transition-opacity" aria-hidden="true" onclick="closeTopItemsModal()"></div>

            <!-- Frame element to trick browser into centering modal contents -->
            <span class="hidden sm:inline-block sm:align-middle sm:h-screen" aria-hidden="true">&#8203;</span>

            <!-- Modal Content box -->
            <div class="inline-block align-bottom bg-white rounded-3xl text-left overflow-hidden shadow-2xl transform transition-all sm:my-8 sm:align-middle sm:max-w-2xl sm:w-full border border-slate-200">
                <div class="bg-indigo-900 px-6 py-4 flex items-center justify-between text-white">
                    <div class="flex items-center gap-2">
                        <span class="text-2xl">🏆</span>
                        <div>
                            <h3 class="text-lg font-bold" id="modal-title">Top Performing Dishes Analysis</h3>
                            <p class="text-xs text-indigo-200">Ranked by overall order volume and compiled transaction metrics</p>
                        </div>
                    </div>
                    <button type="button" onclick="closeTopItemsModal()" class="text-indigo-200 hover:text-white transition-colors outline-none focus:outline-none">
                        <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"></path>
                        </svg>
                    </button>
                </div>

                <div class="p-6 bg-slate-50">
                    <p class="text-xs text-slate-500 mb-4 leading-relaxed">
                        The metrics below highlight which culinary masterpieces are generating the highest volume and cash-flow. Keep these dishes in stock to optimize student engagement!
                    </p>

                    <div class="space-y-4 max-h-[400px] overflow-y-auto pr-1">
                        @forelse($topPerformingItems as $index => $item)
                            <div class="bg-white rounded-2xl p-4 border border-slate-200 shadow-sm flex items-center justify-between gap-4">
                                <div class="flex items-center gap-3">
                                    <div class="h-9 w-9 rounded-xl flex items-center justify-center font-black text-sm border shadow-sm {{ $index === 0 ? 'bg-amber-100 text-amber-700 border-amber-300' : ($index === 1 ? 'bg-slate-100 text-slate-700 border-slate-300' : ($index === 2 ? 'bg-orange-100 text-orange-700 border-orange-300' : 'bg-slate-50 text-slate-500 border-slate-200')) }}">
                                        #{{ $index + 1 }}
                                    </div>
                                    <div>
                                        <p class="font-extrabold text-sm text-slate-800">{{ $item->food_name ?? 'Unnamed Item' }}</p>
                                        <div class="flex items-center gap-2 mt-0.5 text-[11px] text-slate-500">
                                            <span class="bg-slate-100 px-1.5 py-0.5 rounded font-semibold">{{ $item->total_orders }} Orders Placed</span>
                                            <span class="text-slate-300">|</span>
                                            <span>Est. Revenue: <b class="text-indigo-600 font-bold">GH₵ {{ number_format($item->total_revenue, 2) }}</b></span>
                                        </div>
                                    </div>
                                </div>
                                <div class="text-right">
                                    <p class="text-xs text-slate-400 font-semibold tracking-wider uppercase">Quantity Sold</p>
                                    <p class="text-xl font-black text-slate-900 code-font mt-0.5">{{ $item->total_quantity }} <span class="text-xs font-normal text-slate-500">units</span></p>
                                </div>
                            </div>
                        @empty
                            <p class="text-xs text-slate-400 italic text-center py-8">No order performance statistics found. Place orders in client app to populate analysis.</p>
                        @endforelse
                    </div>
                </div>

                <div class="bg-slate-100 px-6 py-4 flex justify-end border-t border-slate-200">
                    <button type="button" onclick="closeTopItemsModal()" class="px-4 py-2 bg-slate-800 hover:bg-slate-900 text-white text-xs font-bold rounded-xl transition-all shadow">
                        Close Analysis
                    </button>
                </div>
            </div>
        </div>
    </div>

    <!-- Interactive QR Code Modal -->
    <div id="qrCodeModal" class="fixed inset-0 z-50 overflow-y-auto hidden" aria-labelledby="qr-modal-title" role="dialog" aria-modal="true">
        <div class="flex items-end justify-center min-h-screen pt-4 px-4 pb-20 text-center sm:block sm:p-0">
            <!-- Background overlay -->
            <div class="fixed inset-0 bg-slate-900 bg-opacity-75 transition-opacity" aria-hidden="true" onclick="closeQrModal()"></div>

            <!-- Frame element to center modal contents -->
            <span class="hidden sm:inline-block sm:align-middle sm:h-screen" aria-hidden="true">&#8203;</span>

            <div class="inline-block align-bottom bg-white rounded-3xl text-left overflow-hidden shadow-2xl transform transition-all sm:my-8 sm:align-middle sm:max-w-md sm:w-full border border-slate-200">
                <div class="bg-indigo-600 px-6 py-4 flex items-center justify-between text-white">
                    <div class="flex items-center gap-2">
                        <span class="text-2xl">📱</span>
                        <div>
                            <h3 class="text-base font-bold" id="qr-modal-title">Student Menu QR Code</h3>
                            <p class="text-[11px] text-indigo-100">Scan at cafeteria counter to order instantly</p>
                        </div>
                    </div>
                    <button type="button" onclick="closeQrModal()" class="text-indigo-200 hover:text-white transition-colors outline-none focus:outline-none">
                        <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"></path>
                        </svg>
                    </button>
                </div>

                <div class="p-6 bg-slate-50 flex flex-col items-center text-center">
                    <h4 id="qrFoodName" class="font-extrabold text-slate-800 text-lg mb-1">Dish Name</h4>
                    <p id="qrFoodPrice" class="text-indigo-600 font-bold text-sm mb-4">GH₵ 0.00</p>
                    
                    <!-- QR Container Card -->
                    <div class="bg-white p-5 rounded-2xl border border-slate-200 shadow-sm flex items-center justify-center mb-4">
                        <div id="qrcode" class="p-2"></div>
                    </div>
                    
                    <p class="text-xs text-slate-400 max-w-xs leading-relaxed mb-6">
                        Print this QR code and paste it on your cafeteria counter. Students scan this to open this dish's checkout page instantly!
                    </p>

                    <div class="w-full space-y-2">
                        <button type="button" id="copyBtn" onclick="copyQrLink()" class="w-full py-2.5 bg-slate-100 hover:bg-slate-200 text-slate-700 text-xs font-bold rounded-xl transition-all border border-slate-200">
                            Copy Order Link
                        </button>
                        <a id="previewBtn" href="#" target="_blank" class="w-full block py-2.5 bg-indigo-600 hover:bg-indigo-700 text-white text-xs font-bold rounded-xl transition-all shadow-xs text-center">
                            Open Checkout Preview
                        </a>
                    </div>
                </div>

                <div class="bg-slate-100 px-6 py-4 flex justify-end border-t border-slate-200">
                    <button type="button" onclick="closeQrModal()" class="px-4 py-2 bg-slate-800 hover:bg-slate-900 text-white text-xs font-bold rounded-xl transition-all shadow">
                        Close
                    </button>
                </div>
            </div>
        </div>
    </div>

    <!-- JavaScript Helpers for Modals & PWA Notifications -->
    <script>
        let qrCodeInstance = null;
        let qrCodeLink = "";

        function openTopItemsModal() {
            document.getElementById('topItemsModal').classList.remove('hidden');
            document.body.classList.add('overflow-hidden');
        }

        function closeTopItemsModal() {
            document.getElementById('topItemsModal').classList.add('hidden');
            document.body.classList.remove('overflow-hidden');
        }

        function generateFoodItemQr(itemId, itemName, itemPrice) {
            document.getElementById('qrFoodName').textContent = itemName;
            document.getElementById('qrFoodPrice').textContent = "GH₵ " + parseFloat(itemPrice).toFixed(2);
            
            // Build absolute URL for the student-facing food item checkout route
            qrCodeLink = window.location.origin + "/api/student/order-item/" + itemId;
            
            // Set href for preview button
            document.getElementById('previewBtn').href = qrCodeLink;
            
            // Reset qrcode container
            const qrContainer = document.getElementById('qrcode');
            qrContainer.innerHTML = "";
            
            // Create a brand new QRCode instance using library
            qrCodeInstance = new QRCode(qrContainer, {
                text: qrCodeLink,
                width: 160,
                height: 160,
                colorDark : "#0f172a", // slate-900
                colorLight : "#ffffff",
                correctLevel : QRCode.CorrectLevel.H
            });
            
            // Display Modal
            document.getElementById('qrCodeModal').classList.remove('hidden');
            document.body.classList.add('overflow-hidden');
        }

        function closeQrModal() {
            document.getElementById('qrCodeModal').classList.add('hidden');
            document.body.classList.remove('overflow-hidden');
        }

        function copyQrLink() {
            if (!qrCodeLink) return;
            navigator.clipboard.writeText(qrCodeLink).then(() => {
                const btn = document.getElementById('copyBtn');
                const origText = btn.textContent;
                btn.textContent = "✓ Link Copied!";
                btn.classList.remove('bg-slate-100', 'text-slate-700');
                btn.classList.add('bg-emerald-500', 'text-white', 'border-emerald-600');
                
                setTimeout(() => {
                    btn.textContent = origText;
                    btn.classList.remove('bg-emerald-500', 'text-white', 'border-emerald-600');
                    btn.classList.add('bg-slate-100', 'text-slate-700');
                }, 2000);
            }).catch(err => {
                alert("Failed to copy link: " + err);
            });
        }

        // PWA Service Worker Registration & Notification Integration
        document.addEventListener("DOMContentLoaded", function() {
            // 1. Service Worker Registration
            if ('serviceWorker' in navigator) {
                navigator.serviceWorker.register('/api/service-worker.js')
                .then(reg => {
                    console.log('PWA Service Worker registered successfully:', reg.scope);
                })
                .catch(err => {
                    console.error('Service Worker registration failed:', err);
                });
            }

            // 2. Request Notification Permissions
            if ('Notification' in window) {
                if (Notification.permission === 'default') {
                    Notification.requestPermission();
                }
            }

            // 3. Setup Order Polling for instant pushes
            const vendorId = "{{ $vendor->id }}";
            
            // Load previously notified orders from LocalStorage to avoid repetitive alerts on page reload
            let notifiedOrders = JSON.parse(localStorage.getItem('notified_order_ids') || '[]');

            function checkIncomingOrders() {
                if (!vendorId) return;
                
                fetch(`/api/vendor/orders/unread-count?vendor_id=${vendorId}`)
                .then(response => response.json())
                .then(data => {
                    if (data && data.orders) {
                        let playSound = false;
                        let newOrdersCount = 0;
                        
                        data.orders.forEach(order => {
                            const orderIdStr = String(order.id);
                            if (!notifiedOrders.includes(orderIdStr)) {
                                playSound = true;
                                newOrdersCount++;
                                notifiedOrders.push(orderIdStr);
                                
                                // Show Notification
                                triggerPushNotification(
                                    `🍲 New Order Received! (#${order.id})`,
                                    `Dish: ${order.food_name || 'Cafeteria Item'} (Qty: ${order.quantity || 1}) - Total: GH₵ ${parseFloat(order.total_price).toFixed(2)}`
                                );
                            }
                        });
                        
                        if (playSound) {
                            // Persist updated list
                            localStorage.setItem('notified_order_ids', JSON.stringify(notifiedOrders));
                            
                            // Play a modern subtle sound to grab attention
                            try {
                                const audioCtx = new (window.AudioContext || window.webkitAudioContext)();
                                const oscillator = audioCtx.createOscillator();
                                const gainNode = audioCtx.createGain();
                                
                                oscillator.connect(gainNode);
                                gainNode.connect(audioCtx.destination);
                                
                                oscillator.type = 'sine';
                                oscillator.frequency.setValueAtTime(587.33, audioCtx.currentTime); // D5 note
                                gainNode.gain.setValueAtTime(0.3, audioCtx.currentTime);
                                
                                oscillator.start();
                                oscillator.stop(audioCtx.currentTime + 0.15);
                                
                                setTimeout(() => {
                                    const osc2 = audioCtx.createOscillator();
                                    osc2.connect(gainNode);
                                    osc2.type = 'sine';
                                    osc2.frequency.setValueAtTime(880.00, audioCtx.currentTime); // A5 note
                                    osc2.start();
                                    osc2.stop(audioCtx.currentTime + 0.3);
                                }, 180);
                            } catch (e) {
                                console.log("Audio alert blocked or unsupported:", e);
                            }
                            
                            // Vibrate if supported
                            if ('vibrate' in navigator) {
                                navigator.vibrate([150, 100, 150]);
                            }
                            
                            // Automatically reload page view to update UI elements without hard manual refresh
                            setTimeout(() => {
                                window.location.reload();
                            }, 3500);
                        }
                    }
                })
                .catch(err => console.error('Error polling for orders:', err));
            }

            function triggerPushNotification(title, body) {
                // If tab is focused, show HTML5 web notification
                if (document.visibilityState === 'visible' && 'Notification' in window && Notification.permission === 'granted') {
                    new Notification(title, {
                        body: body,
                        icon: 'https://img.icons8.com/color/192/hamburger.png'
                    });
                } else if ('serviceWorker' in navigator && navigator.serviceWorker.controller) {
                    // If screen is off or app is backgrounded, send to SW message queue to trigger active push
                    navigator.serviceWorker.controller.postMessage({
                        type: 'NEW_ORDER',
                        title: title,
                        body: body,
                        url: window.location.href
                    });
                }
            }

            // Run check on load, then poll every 10 seconds
            setTimeout(checkIncomingOrders, 2000);
            setInterval(checkIncomingOrders, 10000);
        });
    </script>

</body>
</html>
