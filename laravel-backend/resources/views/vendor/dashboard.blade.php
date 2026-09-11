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
    <!-- html5-qrcode scanner library -->
    <script src="https://unpkg.com/html5-qrcode" type="text/javascript"></script>
    
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

    <!-- Low Stock Toast Container -->
    <div id="lowStockToastContainer" class="fixed top-5 right-5 z-50 flex flex-col gap-3 pointer-events-none max-w-sm w-full"></div>

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


        @if(!empty($lowStockItems))
            <!-- Low Stock Warnings Alert Banner -->
            <div class="mb-8 p-5 bg-amber-50 border-l-4 border-amber-500 rounded-2xl flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 shadow-sm animate-pulse">
                <div class="flex items-start gap-3">
                    <div class="bg-amber-100 text-amber-700 p-2 rounded-xl mt-0.5">
                        <svg class="w-6 h-6 text-amber-600" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z"></path>
                        </svg>
                    </div>
                    <div>
                        <h4 class="text-sm font-extrabold text-amber-900">⚠️ Menu Stock Alert: Low Inventory Detected!</h4>
                        <p class="text-xs text-amber-700 mt-1 leading-relaxed">
                            The following dishes are running extremely low based on their current daily order volumes and thresholds:
                        </p>
                        <div class="flex flex-wrap gap-2 mt-2">
                            @foreach($lowStockItems as $item)
                                <span class="inline-flex items-center gap-1.5 px-3 py-1 bg-amber-100 text-amber-800 text-xs font-extrabold rounded-lg border border-amber-200">
                                    🔴 {{ $item['name'] }} ({{ $item['remaining'] }} remaining)
                                </span>
                            @endforeach
                        </div>
                    </div>
                </div>
                <button type="button" onclick="this.parentElement.remove()" class="text-amber-500 hover:text-amber-800 transition-colors">
                    <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"></path>
                    </svg>
                </button>
            </div>
        @endif


        <!-- Tab Navigation Switcher -->
        <div class="flex border-b border-slate-200 mb-8 overflow-x-auto whitespace-nowrap scrollbar-none">
            <button onclick="switchTab('operations')" id="tabBtn-operations" class="px-6 py-3.5 text-sm font-extrabold flex items-center gap-2 border-b-2 border-indigo-600 text-indigo-600 transition-all duration-200 focus:outline-none">
                🏪 Shop Operations & Catalog
            </button>
            <button onclick="switchTab('analytics')" id="tabBtn-analytics" class="px-6 py-3.5 text-sm font-bold flex items-center gap-2 border-b-2 border-transparent text-slate-500 hover:text-slate-800 transition-all duration-200 focus:outline-none">
                📊 Interactive Performance Analytics
            </button>
        </div>

        <!-- Tab 1 Container: Shop Operations -->
        <div id="tabContent-operations" class="space-y-8">

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
                
                <button onclick="openScannerModal()" type="button" class="w-full mt-4 py-3 px-4 bg-gradient-to-r from-indigo-600 to-purple-600 hover:from-indigo-700 hover:to-purple-700 text-white font-extrabold text-sm rounded-2xl shadow-md hover:shadow-lg transition-all duration-200 flex items-center justify-center gap-2">
                    <svg class="w-5 h-5 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v1m6 11h2m-6 0h-2v4m0-11v3m0 0h.01M12 12h.01M16 20h2M4 12h4m12 0a8 8 0 11-16 0 8 8 0 0116 0z"></path>
                    </svg>
                    📷 Scan Student Claim QR
                </button>
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
                <div id="incoming-orders-console" class="bg-white rounded-3xl border border-slate-200 shadow-sm overflow-hidden mb-2">
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

                    <!-- Search & Filter Bar -->
                    <div class="px-6 py-4 bg-slate-50 border-b border-slate-100">
                        <form action="" method="GET" class="grid grid-cols-1 sm:grid-cols-4 gap-3 items-end">
                            <input type="hidden" name="vendor_id" value="{{ $vendor->id }}">
                            
                            <!-- Search query input -->
                            <div class="flex flex-col gap-1">
                                <label class="text-[10px] font-bold text-slate-500 uppercase tracking-wider">Search Orders</label>
                                <div class="relative">
                                    <input type="text" name="order_search" value="{{ $order_search ?? '' }}" placeholder="Order ID or Student Name..." class="pl-8 pr-3 py-1.5 text-xs rounded-xl border border-slate-300 focus:ring-2 focus:ring-indigo-500 outline-none w-full transition-all">
                                    <svg class="w-3.5 h-3.5 text-slate-400 absolute left-2.5 top-2.5" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"></path>
                                    </svg>
                                </div>
                            </div>

                            <!-- Start Date -->
                            <div class="flex flex-col gap-1">
                                <label class="text-[10px] font-bold text-slate-500 uppercase tracking-wider">Start Date</label>
                                <input type="date" name="start_date" value="{{ $start_date ?? '' }}" class="px-3 py-1.5 text-xs rounded-xl border border-slate-300 focus:ring-2 focus:ring-indigo-500 outline-none w-full text-slate-700 transition-all">
                            </div>

                            <!-- End Date -->
                            <div class="flex flex-col gap-1">
                                <label class="text-[10px] font-bold text-slate-500 uppercase tracking-wider">End Date</label>
                                <input type="date" name="end_date" value="{{ $end_date ?? '' }}" class="px-3 py-1.5 text-xs rounded-xl border border-slate-300 focus:ring-2 focus:ring-indigo-500 outline-none w-full text-slate-700 transition-all">
                            </div>

                            <!-- Actions -->
                            <div class="flex items-center gap-2">
                                <button type="submit" class="flex-1 py-1.5 bg-indigo-600 hover:bg-indigo-700 text-white text-xs font-bold rounded-xl shadow transition-all flex items-center justify-center gap-1.5">
                                    <span>🔍</span> Filter
                                </button>
                                @if(!empty($order_search) || !empty($start_date) || !empty($end_date))
                                    <a href="?vendor_id={{ $vendor->id }}" class="py-1.5 px-3 bg-slate-200 hover:bg-slate-300 text-slate-700 text-xs font-bold rounded-xl transition-all text-center">
                                        Reset
                                    </a>
                                @endif
                            </div>
                        </form>
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
                                        <td class="py-4 px-6 text-center flex items-center justify-center gap-1.5">
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
                                            
                                            <!-- Printable PDF receipt download -->
                                            <a href="/api/orders/{{ $order->id }}/receipt" target="_blank" title="Download Printable PDF Receipt" class="inline-flex items-center justify-center p-1.5 text-slate-500 hover:text-indigo-600 hover:bg-indigo-50 rounded-lg transition-colors border border-slate-200 hover:border-indigo-200">
                                                <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                                                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 10v6m0 0l-3-3m3 3l3-3m2 8H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"></path>
                                                </svg>
                                            </a>
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
                        <p class="text-[11px] text-indigo-600 font-extrabold px-1 flex items-center gap-1.5 animate-pulse">
                            <span>📱</span>
                            <span>Pro-Tip: Swipe right on active mobile orders to instantly complete them!</span>
                        </p>
                        @forelse($orders as $order)
                            @php
                                $currStatus = strtoupper($order->status);
                                $isSwipeable = !in_array($currStatus, ['COMPLETED', 'DECLINED', 'CANCELLED']);
                                $badgeColor = $statusColors[$currStatus] ?? 'bg-slate-50 text-slate-600 border-slate-200';
                            @endphp
                            <div class="relative overflow-hidden rounded-2xl border border-slate-200 bg-white group select-none shadow-3xs animate-fade-in">
                                @if($isSwipeable)
                                    <!-- Background action revealed on swipe right -->
                                    <div class="absolute inset-y-0 left-0 w-full bg-emerald-600 flex items-center pl-6 text-white font-extrabold text-xs transition-opacity duration-150 opacity-0 pointer-events-none animate-pulse" id="swipe-bg-{{ $order->id }}">
                                        <div class="flex items-center gap-2">
                                            <span class="text-base">✓</span>
                                            <span>Swipe right to Complete Order #{{ $order->id }}</span>
                                        </div>
                                    </div>
                                @endif

                                <!-- Foreground Swipeable Card -->
                                <div class="bg-slate-50 p-4 space-y-3 relative z-10 transition-transform duration-150"
                                     id="order-card-{{ $order->id }}"
                                     @if($isSwipeable)
                                         style="touch-action: pan-y;"
                                         ontouchstart="handleTouchStart(event, '{{ $order->id }}')"
                                         ontouchmove="handleTouchMove(event, '{{ $order->id }}')"
                                         ontouchend="handleTouchEnd(event, '{{ $order->id }}')"
                                     @endif>
                                    <div class="flex items-center justify-between">
                                        <div class="flex items-center gap-1.5">
                                            <span class="text-xs font-semibold code-font text-slate-400">#{{ $order->id }}</span>
                                            @if($isSwipeable)
                                                <span class="text-[9px] text-slate-400 font-bold bg-slate-200/50 px-1.5 py-0.5 rounded-md animate-pulse">Swipeable ➔</span>
                                            @endif
                                        </div>
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
                                        <div class="flex items-center gap-1.5 flex-grow justify-end">
                                            <form action="/api/vendor/orders/{{ $order->id }}/update-status" method="POST" class="max-w-[130px]" id="status-form-{{ $order->id }}">
                                                @csrf
                                                <select name="status" onchange="this.form.submit()" class="w-full px-2 py-1.5 text-xs rounded-lg border border-slate-300 bg-slate-50 text-slate-700 font-bold focus:outline-none focus:ring-2 focus:ring-indigo-500 transition-all cursor-pointer">
                                                    <option value="ORDER_PLACED" {{ $currStatus === 'ORDER_PLACED' ? 'selected' : '' }}>Pending</option>
                                                    <option value="PREPARING" {{ $currStatus === 'PREPARING' ? 'selected' : '' }}>Preparing</option>
                                                    <option value="READY" {{ $currStatus === 'READY' ? 'selected' : '' }}>Ready</option>
                                                    <option value="COMPLETED" {{ $currStatus === 'COMPLETED' ? 'selected' : '' }}>Completed</option>
                                                    <option value="DECLINED" {{ $currStatus === 'DECLINED' ? 'selected' : '' }}>Declined</option>
                                                </select>
                                            </form>
                                            <a href="/api/orders/{{ $order->id }}/receipt" target="_blank" title="Download Printable PDF Receipt" class="inline-flex items-center justify-center p-2 text-slate-500 hover:text-indigo-600 hover:bg-indigo-50 rounded-lg transition-colors border border-slate-200 hover:border-indigo-200">
                                                <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                                                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 10v6m0 0l-3-3m3 3l3-3m2 8H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"></path>
                                                </svg>
                                            </a>
                                        </div>
                                    </div>
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
                                    <th class="py-4 px-4 text-center">Daily Stock</th>
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
                                            @if($food->is_low_stock)
                                                <span class="inline-flex items-center gap-1.5 px-2.5 py-1 bg-rose-50 text-rose-700 text-xs font-extrabold rounded-lg border border-rose-100 animate-pulse" title="Running low on ingredients!">
                                                    ⚠️ {{ $food->remaining_stock }} / {{ $food->initial_stock }}
                                                </span>
                                            @else
                                                <span class="inline-flex items-center gap-1.5 px-2.5 py-1 bg-slate-100 text-slate-700 text-xs font-semibold rounded-lg border border-slate-200">
                                                    📦 {{ $food->remaining_stock }} / {{ $food->initial_stock }}
                                                </span>
                                            @endif
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
                                                <!-- Adjust Inventory Thresholds button -->
                                                <button type="button" onclick="openInventoryModal('{{ $food->id }}', '{{ addslashes($food->name) }}', '{{ $food->initial_stock }}', '{{ $food->low_stock_threshold }}')" class="p-1.5 bg-slate-100 text-slate-600 hover:bg-emerald-50 hover:text-emerald-700 rounded-lg border border-slate-200 hover:border-emerald-200 transition-all shadow-sm flex items-center justify-center" title="Adjust Inventory Thresholds">
                                                    <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                                                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 6V4m0 2a2 2 0 100 4m0-4a2 2 0 110 4m-6 8a2 2 0 100-4m0 4a2 2 0 110-4m0 4v2m0-6V4m6 6v10m6-2a2 2 0 100-4m0 4a2 2 0 110-4m0 4v2m0-6V4"></path>
                                                    </svg>
                                                </button>

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
                                    <div class="flex items-center gap-1.5">
                                        @if($food->is_low_stock)
                                            <span class="px-2 py-1 bg-rose-50 text-rose-700 text-[9px] font-extrabold rounded-md border border-rose-100 animate-pulse">
                                                Low Stock: {{ $food->remaining_stock }}
                                            </span>
                                        @else
                                            <span class="px-2 py-1 bg-slate-100 text-slate-700 text-[9px] font-semibold rounded-md border border-slate-200">
                                                Stock: {{ $food->remaining_stock }} / {{ $food->initial_stock }}
                                            </span>
                                        @endif
                                        <strong class="text-indigo-600 font-extrabold text-sm code-font">GH₵ {{ number_format($food->price, 2) }}</strong>
                                    </div>
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
                                        <!-- Adjust Inventory Thresholds button -->
                                        <button type="button" onclick="openInventoryModal('{{ $food->id }}', '{{ addslashes($food->name) }}', '{{ $food->initial_stock }}', '{{ $food->low_stock_threshold }}')" class="px-2.5 py-1.5 bg-slate-50 text-slate-600 border border-slate-200 text-xs font-bold rounded-xl flex items-center gap-1 transition-all" style="min-height: 40px;">
                                            <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 6V4m0 2a2 2 0 100 4m0-4a2 2 0 110 4m-6 8a2 2 0 100-4m0 4a2 2 0 110-4m0 4v2m0-6V4m6 6v10m6-2a2 2 0 100-4m0 4a2 2 0 110-4m0 4v2m0-6V4"></path>
                                            </svg>
                                            <span>Stock</span>
                                        </button>

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

                        <div class="grid grid-cols-2 gap-4">
                            <div>
                                <label class="block text-xs font-bold text-slate-600 uppercase tracking-wider mb-1">Daily Prep Limit (Stock)</label>
                                <input type="number" name="initial_stock" value="{{ old('initial_stock', 50) }}" placeholder="50" required class="w-full px-4 py-2.5 text-sm rounded-xl border border-slate-300 focus:ring-2 focus:ring-indigo-500 outline-none transition-all code-font">
                                <p class="text-[9px] text-slate-400 mt-1">Total quantity prepared per day</p>
                            </div>
                            <div>
                                <label class="block text-xs font-bold text-slate-600 uppercase tracking-wider mb-1">Low Stock Alert at</label>
                                <input type="number" name="low_stock_threshold" value="{{ old('low_stock_threshold', 10) }}" placeholder="10" required class="w-full px-4 py-2.5 text-sm rounded-xl border border-slate-300 focus:ring-2 focus:ring-indigo-500 outline-none transition-all code-font">
                                <p class="text-[9px] text-slate-400 mt-1">Notify when remaining stock <= this</p>
                            </div>
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
        </div> <!-- End of tabContent-operations -->

        <!-- Tab 2 Container: Interactive Analytics Dashboard -->
        <div id="tabContent-analytics" class="hidden space-y-8">
            
            <!-- Analytics Welcome & Highlights Banner -->
            <div class="bg-gradient-to-r from-slate-900 via-indigo-950 to-slate-900 text-white rounded-3xl p-6 shadow-xl relative overflow-hidden">
                <div class="absolute right-0 bottom-0 opacity-10 transform translate-x-12 translate-y-12">
                    <svg class="w-80 h-80 text-white" fill="currentColor" viewBox="0 0 20 20">
                        <path d="M2 10a8 8 0 018-8v8h8a8 8 0 11-16 0z"></path>
                        <path d="M12 2.252A8.014 8.014 0 0117.748 8H12V2.252z"></path>
                    </svg>
                </div>
                <div class="relative z-10">
                    <div class="flex items-center gap-2 mb-2">
                        <span class="px-3 py-1 bg-indigo-500/20 text-indigo-300 font-extrabold text-xs tracking-wider rounded-full uppercase border border-indigo-500/30">
                            📊 DATA INTELLIGENCE
                        </span>
                        <span class="text-xs text-slate-400 font-medium">Updated Real-Time</span>
                    </div>
                    <h3 class="text-2xl md:text-3xl font-extrabold text-slate-100">Performance & Analytics Hub</h3>
                    <p class="text-slate-300 mt-2 text-sm max-w-2xl leading-relaxed">
                        In-depth sales, feedback, and fulfillment analysis. Use these data visualizations to identify popular items, optimize menu pricing, and enhance preparation efficiency.
                    </p>
                </div>
            </div>

            <!-- Analytics Key Metrics Grid -->
            <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-5 gap-5">
                
                <!-- KPI 1: Total Orders Processed -->
                <div class="bg-white rounded-2xl p-5 shadow-sm border border-slate-200 flex flex-col justify-between hover:shadow-md transition-shadow">
                    <div class="flex items-center gap-3">
                        <div class="bg-indigo-50 text-indigo-600 p-2.5 rounded-xl">
                            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2"></path>
                            </svg>
                        </div>
                        <div>
                            <span class="text-[11px] font-bold text-slate-400 uppercase tracking-wider">Completed Orders</span>
                            <h4 class="text-xl font-extrabold text-slate-900 mt-0.5 code-font">{{ $metrics['order_metrics']['completed_orders_count'] ?? 0 }}</h4>
                        </div>
                    </div>
                    <div class="mt-4 border-t border-slate-100 pt-3 flex items-center justify-between text-xs">
                        <span class="text-slate-500">Total Placed:</span>
                        <strong class="text-indigo-600 font-bold code-font">{{ $metrics['order_metrics']['total_orders_placed'] ?? 0 }}</strong>
                    </div>
                </div>

                <!-- KPI 2: Average Order Value (AOV) -->
                <div class="bg-white rounded-2xl p-5 shadow-sm border border-slate-200 flex flex-col justify-between hover:shadow-md transition-shadow">
                    <div class="flex items-center gap-3">
                        <div class="bg-emerald-50 text-emerald-600 p-2.5 rounded-xl">
                            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8c-1.657 0-3 .895-3 2s1.343 2 3 2 3 .895 3 2-1.343 2-3 2m0-8c1.11 0 2.08.402 2.599 1M12 8V7m0 1v8m0 0v1m0-1c-1.11 0-2.08-.402-2.599-1M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path>
                            </svg>
                        </div>
                        <div>
                            <span class="text-[11px] font-bold text-slate-400 uppercase tracking-wider">Avg Order Value</span>
                            <h4 class="text-xl font-extrabold text-slate-900 mt-0.5 code-font">GH₵ {{ number_format($metrics['order_metrics']['average_order_value'] ?? 0, 2) }}</h4>
                        </div>
                    </div>
                    <div class="mt-4 border-t border-slate-100 pt-3 flex items-center justify-between text-xs">
                        <span class="text-slate-500">Total Revenue:</span>
                        <strong class="text-emerald-600 font-bold code-font">GH₵ {{ number_format($metrics['order_metrics']['total_completed_revenue'] ?? 0, 2) }}</strong>
                    </div>
                </div>

                <!-- KPI 3: Popular Food Item -->
                <div class="bg-white rounded-2xl p-5 shadow-sm border border-slate-200 flex flex-col justify-between hover:shadow-md transition-shadow">
                    <div class="flex items-center gap-3">
                        <div class="bg-amber-50 text-amber-600 p-2.5 rounded-xl">
                            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 3v4M3 5h4M6 17v4m-2-2h4m5-16l2.286 6.857L21 12l-5.714 2.143L13 21l-2.286-6.857L5 12l5.714-2.143L13 3z"></path>
                            </svg>
                        </div>
                        <div class="overflow-hidden">
                            <span class="text-[11px] font-bold text-slate-400 uppercase tracking-wider">Popular Dish</span>
                            <h4 class="text-sm font-extrabold text-slate-900 mt-0.5 truncate" title="{{ isset($topPerformingItems[0]) ? $topPerformingItems[0]->food_name : 'N/A' }}">
                                {{ isset($topPerformingItems[0]) ? $topPerformingItems[0]->food_name : 'N/A' }}
                            </h4>
                        </div>
                    </div>
                    <div class="mt-4 border-t border-slate-100 pt-3 flex items-center justify-between text-xs">
                        <span class="text-slate-500">Units Sold:</span>
                        <strong class="text-amber-600 font-bold code-font">{{ isset($topPerformingItems[0]) ? (int)$topPerformingItems[0]->total_quantity : 0 }}</strong>
                    </div>
                </div>

                <!-- KPI 4: Customer Feedback Rating -->
                <div class="bg-white rounded-2xl p-5 shadow-sm border border-slate-200 flex flex-col justify-between hover:shadow-md transition-shadow">
                    <div class="flex items-center gap-3">
                        <div class="bg-rose-50 text-rose-600 p-2.5 rounded-xl">
                            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11.049 2.927c.3-.921 1.603-.921 1.902 0l1.519 4.674a1 1 0 00.95.69h4.915c.969 0 1.371 1.24.588 1.81l-3.976 2.888a1 1 0 00-.363 1.118l1.518 4.674c.3.922-.755 1.688-1.538 1.118l-3.976-2.888a1 1 0 00-1.176 0l-3.976 2.888c-.783.57-1.838-.197-1.538-1.118l1.518-4.674a1 1 0 00-.363-1.118l-3.976-2.888c-.784-.57-.38-1.81.588-1.81h4.914a1 1 0 00.951-.69l1.519-4.674z"></path>
                            </svg>
                        </div>
                        <div>
                            <span class="text-[11px] font-bold text-slate-400 uppercase tracking-wider">Quality Score</span>
                            <h4 class="text-xl font-extrabold text-slate-900 mt-0.5 code-font">{{ $metrics['rating_metrics']['overall_average_rating'] ?? '0.0' }} / 5.0</h4>
                        </div>
                    </div>
                    <div class="mt-4 border-t border-slate-100 pt-3 flex items-center justify-between text-xs">
                        <span class="text-slate-500">Reviews:</span>
                        <strong class="text-rose-600 font-bold code-font">{{ $metrics['rating_metrics']['total_feedback_count'] ?? 0 }}</strong>
                    </div>
                </div>

                <!-- KPI 5: Average Order Prep Time -->
                <div class="bg-white rounded-2xl p-5 shadow-sm border border-slate-200 flex flex-col justify-between hover:shadow-md transition-shadow">
                    <div class="flex items-center gap-3">
                        <div class="bg-blue-50 text-blue-600 p-2.5 rounded-xl">
                            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z"></path>
                            </svg>
                        </div>
                        <div>
                            <span class="text-[11px] font-bold text-slate-400 uppercase tracking-wider">Avg Prep Time</span>
                            <h4 class="text-lg font-extrabold text-slate-900 mt-0.5 code-font">{{ $metrics['completion_time_metrics']['average_formatted'] ?? '0s' }}</h4>
                        </div>
                    </div>
                    <div class="mt-4 border-t border-slate-100 pt-3 flex items-center justify-between text-xs">
                        <span class="text-slate-500">Fastest Prep:</span>
                        <strong class="text-blue-600 font-bold text-[11px] code-font">{{ $metrics['completion_time_metrics']['fastest_formatted'] ?? 'N/A' }}</strong>
                    </div>
                </div>

            </div>

            <!-- Visualization Charts Grid -->
            <div class="grid grid-cols-1 lg:grid-cols-2 gap-8">
                
                <!-- Chart 1: Sales & Orders Volume -->
                <div class="bg-white rounded-3xl border border-slate-200 shadow-sm p-6 flex flex-col justify-between h-[360px]">
                    <div>
                        <h3 class="text-base font-bold text-slate-900 flex items-center gap-2">
                            <span>📈</span> Sales & Volume Trend (Past 7 Days)
                        </h3>
                        <p class="text-xs text-slate-400 mt-0.5">Overview of daily revenue and transaction volume completed in the cafeteria.</p>
                    </div>
                    <div class="relative w-full h-[240px] mt-4">
                        <canvas id="salesAndOrdersChart"></canvas>
                    </div>
                </div>

                <!-- Chart 2: Popular Menu Items share -->
                <div class="bg-white rounded-3xl border border-slate-200 shadow-sm p-6 flex flex-col justify-between h-[360px]">
                    <div>
                        <h3 class="text-base font-bold text-slate-900 flex items-center gap-2">
                            <span>🍩</span> Top Menu Offerings Popularity
                        </h3>
                        <p class="text-xs text-slate-400 mt-0.5">Quantity of dishes sold and the relative share of each menu item.</p>
                    </div>
                    <div class="relative w-full h-[240px] mt-4 flex items-center justify-center">
                        @if(!$topPerformingItems->isEmpty())
                            <canvas id="popularItemsChart"></canvas>
                        @else
                            <div class="text-slate-400 text-xs text-center">
                                <span class="text-3xl block mb-2">🍽️</span>
                                No completed orders available to plot popularity chart.
                            </div>
                        @endif
                    </div>
                </div>

                <!-- Chart 3: Detailed Feedback Breakdown -->
                <div class="bg-white rounded-3xl border border-slate-200 shadow-sm p-6 flex flex-col justify-between h-[360px]">
                    <div>
                        <h3 class="text-base font-bold text-slate-900 flex items-center gap-2">
                            <span>⭐</span> Customer Feedback Dimensions
                        </h3>
                        <p class="text-xs text-slate-400 mt-0.5">Multidimensional student ratings evaluating food quality, speed, and service value.</p>
                    </div>
                    <div class="relative w-full h-[240px] mt-4">
                        @if(($metrics['rating_metrics']['total_feedback_count'] ?? 0) > 0)
                            <canvas id="feedbackDimensionsChart"></canvas>
                        @else
                            <div class="text-slate-400 text-xs text-center flex flex-col items-center justify-center h-full">
                                <span class="text-3xl block mb-2">⭐</span>
                                No feedback reviews submitted yet.
                            </div>
                        @endif
                    </div>
                </div>

                <!-- Chart 4: Fulfillment Status Breakdown -->
                <div class="bg-white rounded-3xl border border-slate-200 shadow-sm p-6 flex flex-col justify-between h-[360px]">
                    <div>
                        <h3 class="text-base font-bold text-slate-900 flex items-center gap-2">
                            <span>⏱️</span> Order Fulfillment Status Distribution
                        </h3>
                        <p class="text-xs text-slate-400 mt-0.5">Distribution of all active and historic orders across different prep statuses.</p>
                    </div>
                    <div class="relative w-full h-[240px] mt-4">
                        <canvas id="fulfillmentStatusChart"></canvas>
                    </div>
                </div>

                <!-- Chart 5: Dynamic Revenue & Volume Trend Chart with Date Range Picker -->
                <div class="bg-white rounded-3xl border border-slate-200 shadow-sm p-6 flex flex-col justify-between lg:col-span-2">
                    <div class="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 border-b border-slate-100 pb-4 mb-4">
                        <div>
                            <h3 class="text-base font-bold text-slate-900 flex items-center gap-2">
                                <span>📅</span> Dynamic Revenue & Volume Trend Chart
                            </h3>
                            <p class="text-xs text-slate-400 mt-0.5 font-medium">Aggregated daily sales revenue and order volumes across any custom date range.</p>
                        </div>
                        <!-- Date Range Picker Component -->
                        <div class="flex flex-wrap items-center gap-2">
                            <div class="flex items-center gap-1.5 bg-slate-50 border border-slate-200 rounded-xl px-2.5 py-1.5 shadow-xs">
                                <span class="text-[10px] text-slate-400 font-bold uppercase tracking-wider">From</span>
                                <input type="date" id="chart-start-date" value="{{ now()->subDays(29)->format('Y-m-d') }}" onchange="applyChartDateRange()" class="bg-transparent border-none text-xs text-slate-800 font-bold focus:ring-0 outline-none code-font p-0" style="width: 105px;">
                            </div>
                            <div class="flex items-center gap-1.5 bg-slate-50 border border-slate-200 rounded-xl px-2.5 py-1.5 shadow-xs">
                                <span class="text-[10px] text-slate-400 font-bold uppercase tracking-wider">To</span>
                                <input type="date" id="chart-end-date" value="{{ now()->format('Y-m-d') }}" onchange="applyChartDateRange()" class="bg-transparent border-none text-xs text-slate-800 font-bold focus:ring-0 outline-none code-font p-0" style="width: 105px;">
                            </div>
                            <button type="button" onclick="applyChartDateRange()" class="px-3 py-1.5 bg-indigo-600 hover:bg-indigo-700 text-white text-xs font-bold rounded-xl shadow-md transition-all flex items-center gap-1.5" style="min-height: 34px;" title="Update Chart Data">
                                <svg class="w-3.5 h-3.5 animate-spin-hover" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 1121.21 15H19"></path>
                                </svg>
                                <span class="hidden sm:inline">Refresh</span>
                            </button>
                        </div>
                    </div>
                    <div class="relative w-full h-[280px]">
                        <canvas id="monthlyRevenueTrendChart"></canvas>
                    </div>
                </div>

            </div>

            <!-- AI-Powered Staffing Insights Section -->
            <div class="bg-white rounded-3xl border border-slate-200 shadow-sm p-6 flex flex-col justify-between">
                <div class="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 border-b border-slate-100 pb-4 mb-5">
                    <div>
                        <div class="flex items-center gap-1.5">
                            <span class="flex h-2 w-2 relative">
                                <span class="animate-ping absolute inline-flex h-full w-full rounded-full bg-emerald-400 opacity-75"></span>
                                <span class="relative inline-flex rounded-full h-2 w-2 bg-emerald-500"></span>
                            </span>
                            <span class="px-2.5 py-0.5 bg-indigo-50 text-indigo-700 text-[10px] font-bold rounded-full uppercase tracking-wider border border-indigo-100">
                                AI Predictive Engine
                            </span>
                        </div>
                        <h3 class="text-base font-extrabold text-slate-900 mt-1 flex items-center gap-2">
                            <span>🤖</span> AI-Powered Staffing & Demand Insights
                        </h3>
                        <p class="text-xs text-slate-400 mt-0.5">Optimal scheduling and chef-allocation guidelines calculated from predicted peak order traffic.</p>
                    </div>

                    <!-- Campus Events Modifier Selector -->
                    <div class="flex items-center gap-2 bg-slate-50 border border-slate-200 rounded-xl px-3 py-1.5 shadow-xs">
                        <label for="campus-traffic-modifier" class="text-[10px] text-slate-400 font-extrabold uppercase tracking-wider">Campus Activity Level</label>
                        <select id="campus-traffic-modifier" onchange="recalculateStaffingInsights()" class="bg-transparent border-none text-xs text-slate-800 font-extrabold focus:ring-0 outline-none p-0 cursor-pointer">
                            <option value="1.0">Standard Day (Normal Traffic)</option>
                            <option value="1.3">Midterm / Exam Week (+30%)</option>
                            <option value="1.8">Matriculation / Graduation (+80%)</option>
                            <option value="0.5">Vacation / Semester Break (-50%)</option>
                        </select>
                    </div>
                </div>

                <!-- Forecast Cards Grid -->
                <div class="grid grid-cols-1 md:grid-cols-3 gap-6">
                    
                    <!-- Insight 1: Peak Hours -->
                    <div class="bg-gradient-to-br from-indigo-50 to-indigo-100/50 rounded-2xl p-5 border border-indigo-100">
                        <div class="flex items-center justify-between mb-3">
                            <h4 class="text-xs font-extrabold text-indigo-900 uppercase tracking-wider">Predicted Peak Hours</h4>
                            <span class="px-2 py-0.5 bg-rose-500 text-white font-extrabold text-[9px] rounded-full uppercase tracking-wider animate-pulse">Critical</span>
                        </div>
                        <p class="text-3xl font-black text-indigo-950 tracking-tight code-font" id="peak-hours-time">12:00 PM - 02:00 PM</p>
                        <p class="text-xs text-indigo-700 font-bold mt-1.5" id="peak-hours-volume">Estimated lunch rush: 45+ orders/hour</p>
                        <div class="mt-4 bg-white/80 rounded-xl p-3 border border-indigo-100/40 text-xs text-indigo-900 leading-relaxed">
                            💡 <strong class="font-bold">AI Note:</strong> Student traffic will surge immediately following afternoon lecture block dismissals. Pre-package popular items!
                        </div>
                    </div>

                    <!-- Insight 2: Staffing Level -->
                    <div class="bg-gradient-to-br from-emerald-50 to-emerald-100/50 rounded-2xl p-5 border border-emerald-100">
                        <div class="flex items-center justify-between mb-3">
                            <h4 class="text-xs font-extrabold text-emerald-900 uppercase tracking-wider">Optimal Staffing Level</h4>
                            <span class="px-2 py-0.5 bg-emerald-600 text-white font-extrabold text-[9px] rounded-full uppercase tracking-wider">Recommended</span>
                        </div>
                        <p class="text-3xl font-black text-emerald-950 tracking-tight code-font" id="optimal-staff-count">5 Cooks / Cashiers</p>
                        <p class="text-xs text-emerald-700 font-bold mt-1.5" id="staff-utilization-rating">Capacity utilization: 88%</p>
                        <div class="mt-4 bg-white/80 rounded-xl p-3 border border-emerald-100/40 text-xs text-emerald-900 leading-relaxed">
                            👥 <strong class="font-bold">Staff Breakdown:</strong> 2 chefs on assembly, 1 order packing clerk, 1 wallet PIN scanner verification attendant.
                        </div>
                    </div>

                    <!-- Insight 3: Menu Preparation Guidance -->
                    <div class="bg-gradient-to-br from-amber-50 to-amber-100/50 rounded-2xl p-5 border border-amber-100">
                        <div class="flex items-center justify-between mb-3">
                            <h4 class="text-xs font-extrabold text-amber-900 uppercase tracking-wider">Demand Prep Recommendation</h4>
                            <span class="px-2 py-0.5 bg-amber-600 text-white font-extrabold text-[9px] rounded-full uppercase tracking-wider">Menu Strategy</span>
                        </div>
                        <p class="text-3xl font-black text-amber-950 tracking-tight code-font" id="recommended-prep-qty">35 Portions Jollof</p>
                        <p class="text-xs text-amber-700 font-bold mt-1.5" id="recommended-prep-details">Prepare 15 Portions Waakye, 12 Drinks</p>
                        <div class="mt-4 bg-white/80 rounded-xl p-3 border border-amber-100/40 text-xs text-amber-900 leading-relaxed">
                            🍲 <strong class="font-bold">Stock Warning:</strong> Avoid wastage by staggering Waakye prep prior to 11:30 AM based on student trend history.
                        </div>
                    </div>

                </div>

                <!-- Secondary Staffing Shift Schedule Table -->
                <div class="mt-6 border border-slate-100 rounded-2xl overflow-hidden bg-slate-50/50">
                    <div class="px-4 py-3 bg-slate-100/80 border-b border-slate-200 flex justify-between items-center">
                        <h4 class="text-xs font-extrabold text-slate-800 uppercase tracking-wider flex items-center gap-1.5">
                            <span>📋</span> Predicted Hourly Demand Curve & Staff Allocation Suggestions
                        </h4>
                        <span class="text-[10px] text-slate-500 font-medium">Model: Gemini 3.5 Flash (Fitted)</span>
                    </div>
                    <div class="overflow-x-auto">
                        <table class="w-full text-left text-xs border-collapse">
                            <thead>
                                <tr class="bg-slate-50 text-slate-400 font-extrabold uppercase tracking-wider border-b border-slate-100">
                                    <th class="py-3 px-4">Shift Segment</th>
                                    <th class="py-3 px-4">Target Hours</th>
                                    <th class="py-3 px-4 text-center">Predicted Traffic</th>
                                    <th class="py-3 px-4 text-center">Recommended Cooks</th>
                                    <th class="py-3 px-4 text-center">Recommended Cashiers</th>
                                    <th class="py-3 px-4 text-right">Status</th>
                                </tr>
                            </thead>
                            <tbody class="divide-y divide-slate-100">
                                <tr>
                                    <td class="py-3 px-4 font-bold text-slate-800">Breakfast Shift</td>
                                    <td class="py-3 px-4 text-slate-600">07:30 AM - 10:00 AM</td>
                                    <td class="py-3 px-4 text-center text-indigo-600 font-bold" id="traffic-breakfast">Moderate (15 ord/hr)</td>
                                    <td class="py-3 px-4 text-center font-extrabold text-slate-900" id="cooks-breakfast">2 Chefs</td>
                                    <td class="py-3 px-4 text-center font-extrabold text-slate-900" id="cashiers-breakfast">1 Clerk</td>
                                    <td class="py-3 px-4 text-right">
                                        <span class="px-2 py-0.5 bg-blue-50 text-blue-700 font-extrabold text-[9px] rounded-md uppercase border border-blue-100">Standard</span>
                                    </td>
                                </tr>
                                <tr>
                                    <td class="py-3 px-4 font-bold text-slate-800">Lunch Peak</td>
                                    <td class="py-3 px-4 text-slate-600">11:30 AM - 02:30 PM</td>
                                    <td class="py-3 px-4 text-center text-rose-600 font-bold" id="traffic-lunch">CRITICAL (45 ord/hr)</td>
                                    <td class="py-3 px-4 text-center font-extrabold text-slate-900" id="cooks-lunch">3 Chefs</td>
                                    <td class="py-3 px-4 text-center font-extrabold text-slate-900" id="cashiers-lunch">2 Clerks</td>
                                    <td class="py-3 px-4 text-right">
                                        <span class="px-2 py-0.5 bg-rose-50 text-rose-700 font-extrabold text-[9px] rounded-md uppercase border border-rose-100">Peak Rush</span>
                                    </td>
                                </tr>
                                <tr>
                                    <td class="py-3 px-4 font-bold text-slate-800">Afternoon Slack</td>
                                    <td class="py-3 px-4 text-slate-600">03:00 PM - 06:00 PM</td>
                                    <td class="py-3 px-4 text-center text-emerald-600 font-bold" id="traffic-afternoon">Light (8 ord/hr)</td>
                                    <td class="py-3 px-4 text-center font-extrabold text-slate-900" id="cooks-afternoon">1 Chef</td>
                                    <td class="py-3 px-4 text-center font-extrabold text-slate-900" id="cashiers-afternoon">1 Clerk</td>
                                    <td class="py-3 px-4 text-right">
                                        <span class="px-2 py-0.5 bg-emerald-50 text-emerald-700 font-extrabold text-[9px] rounded-md uppercase border border-emerald-100">Standby</span>
                                    </td>
                                </tr>
                            </tbody>
                        </table>
                    </div>
                </div>
            </div>

            <div class="h-6"></div>

            <!-- Gemini AI Student Sentiment Analysis -->
            <div class="bg-white rounded-3xl border border-indigo-200 shadow-sm overflow-hidden mb-6">
                <div class="bg-gradient-to-r from-indigo-50 to-purple-50 px-6 py-5 border-b border-indigo-100 flex items-center justify-between">
                    <div>
                        <h3 class="text-base font-bold text-indigo-950 flex items-center gap-2">
                            <span>✨</span> Gemini AI Student Sentiment Analysis
                        </h3>
                        <p class="text-xs text-indigo-600 mt-0.5">Real-time cognitive synthesis of student remarks to identify areas for service improvement.</p>
                    </div>
                    <button type="button" onclick="loadSentimentReport()" class="p-1.5 bg-white text-indigo-600 hover:bg-indigo-100 rounded-lg border border-indigo-200 transition-all shadow-sm flex items-center justify-center gap-1.5 text-xs font-bold" title="Recalculate Sentiment Analysis">
                        <svg class="w-4 h-4 animate-spin-hover" id="sentiment-refresh-icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 1121.21 8H17"></path>
                        </svg>
                        Refresh Insights
                    </button>
                </div>
                <div class="p-6">
                    <div id="sentiment-report-content" class="text-slate-700 text-xs leading-relaxed space-y-4">
                        <div class="flex items-center gap-3 py-8 justify-center text-slate-400">
                            <svg class="w-5 h-5 animate-spin text-indigo-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                                <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                            </svg>
                            <span>Querying Gemini Cognitive Processor...</span>
                        </div>
                    </div>
                </div>
            </div>

            <!-- Customer Reviews & Comments Table -->
            <div class="bg-white rounded-3xl border border-slate-200 shadow-sm overflow-hidden">
                <div class="px-6 py-5 border-b border-slate-100">
                    <h3 class="text-base font-bold text-slate-900 flex items-center gap-2">
                        <span>💬</span> Recent Student Feedback Comments
                    </h3>
                    <p class="text-xs text-slate-400 mt-0.5">Read recent remarks and ratings posted by students on your dishes.</p>
                </div>
                <div class="overflow-x-auto">
                    <table class="w-full text-left border-collapse">
                        <thead>
                            <tr class="bg-slate-50 border-b border-slate-200 text-[11px] text-slate-400 font-extrabold uppercase tracking-wider">
                                <th class="py-4 px-6">Student</th>
                                <th class="py-4 px-4">Rating Breakdown</th>
                                <th class="py-4 px-4">Comment</th>
                                <th class="py-4 px-6 text-right">Submitted At</th>
                            </tr>
                        </thead>
                        <tbody class="divide-y divide-slate-100 text-sm">
                            @forelse($metrics['recent_customer_feedback'] ?? [] as $feedback)
                                <tr class="hover:bg-slate-50/50 transition-colors duration-150">
                                    <td class="py-4 px-6 font-bold text-slate-800">
                                        {{ $feedback['customer_name'] }}
                                    </td>
                                    <td class="py-4 px-4 text-xs">
                                        <div class="grid grid-cols-2 gap-x-4 gap-y-1 text-[11px]">
                                            <div>🍔 Quality: <span class="font-extrabold text-indigo-600">{{ $feedback['ratings']['food_quality'] }}</span>/5</div>
                                            <div>🧹 Clean: <span class="font-extrabold text-emerald-600">{{ $feedback['ratings']['cleanliness'] }}</span>/5</div>
                                            <div>⚡ Speed: <span class="font-extrabold text-amber-600">{{ $feedback['ratings']['service_speed'] }}</span>/5</div>
                                            <div>💰 Value: <span class="font-extrabold text-rose-600">{{ $feedback['ratings']['price_value'] }}</span>/5</div>
                                        </div>
                                        <div class="mt-1.5 flex items-center gap-1">
                                            <span class="text-yellow-400 text-xs">★</span>
                                            <span class="font-bold text-slate-700">Average: {{ $feedback['ratings']['average'] }}/5</span>
                                        </div>
                                    </td>
                                    <td class="py-4 px-4 text-xs italic text-slate-600 max-w-[300px] whitespace-normal leading-relaxed">
                                        "{{ $feedback['comment'] ?? 'No written comment left.' }}"
                                    </td>
                                    <td class="py-4 px-6 text-right text-xs text-slate-400 font-medium">
                                        {{ $feedback['submitted_at'] }}
                                    </td>
                                </tr>
                            @empty
                                <tr>
                                    <td colspan="4" class="py-10 text-center text-slate-400 text-xs font-semibold">
                                        No customer feedback remarks found.
                                    </td>
                                </tr>
                            @endforelse
                        </tbody>
                    </table>
                </div>
            </div>

        </div> <!-- End of tabContent-analytics -->

        <!-- System Diagnostics & Logging Console -->
        <div class="mt-12 bg-white rounded-3xl border border-slate-200 shadow-sm overflow-hidden" id="system-diagnostics-console">
            <div class="p-6 bg-gradient-to-r from-slate-900 to-indigo-950 text-white flex flex-col md:flex-row md:items-center justify-between gap-4">
                <div class="flex items-center gap-3">
                    <div class="p-2.5 bg-indigo-600/30 rounded-xl border border-indigo-400/30 text-indigo-400">
                        <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z"></path>
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"></path>
                        </svg>
                    </div>
                    <div>
                        <h3 class="text-lg font-extrabold tracking-tight flex items-center gap-2">
                            🔧 Central Diagnostics & System Log Auditing
                        </h3>
                        <p class="text-xs text-slate-300">Monitor database connectivity, cache health, and examine active application errors.</p>
                    </div>
                </div>
                
                <!-- Action Buttons -->
                <div class="flex flex-wrap items-center gap-2">
                    <button type="button" onclick="fetchSystemHealth()" class="px-3.5 py-1.5 bg-indigo-500/20 hover:bg-indigo-500/30 text-indigo-300 border border-indigo-500/30 text-xs font-bold rounded-xl transition-all flex items-center gap-1.5">
                        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 1121.248 8H18.25"></path></svg>
                        Test Health
                    </button>
                    <button type="button" onclick="fetchLogs()" class="px-3.5 py-1.5 bg-emerald-500/20 hover:bg-emerald-500/30 text-emerald-300 border border-emerald-500/30 text-xs font-bold rounded-xl transition-all flex items-center gap-1.5">
                        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 1121.248 8H18.25"></path></svg>
                        Reload Logs
                    </button>
                    <button type="button" onclick="clearLogs()" class="px-3.5 py-1.5 bg-rose-500/20 hover:bg-rose-500/30 text-rose-300 border border-rose-500/30 text-xs font-bold rounded-xl transition-all flex items-center gap-1.5">
                        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"></path></svg>
                        Clear Logs
                    </button>
                </div>
            </div>

            <!-- Health Status Panel -->
            <div class="p-6 border-b border-slate-100 bg-slate-50/50">
                <h4 class="text-xs font-bold uppercase text-slate-400 tracking-wider mb-4 font-semibold">Real-Time Connectivity & Driver Health</h4>
                <div class="grid grid-cols-1 md:grid-cols-3 gap-6">
                    <!-- DB Card -->
                    <div class="bg-white rounded-2xl p-4 border border-slate-200 flex items-center gap-3 shadow-3xs" id="db_health_card">
                        <div class="p-2 bg-indigo-50 text-indigo-600 rounded-xl" id="db_health_icon">
                            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 7v10c0 2.21 3.582 4 8 4s8-1.79 8-4V7M4 7c0 2.21 3.582 4 8 4s8-1.79 8-4M4 7c0-2.21 3.582-4 8-4s8 1.79 8 4m0 5c0 2.21-3.582 4-8 4s-8-1.79-8-4"></path></svg>
                        </div>
                        <div>
                            <p class="text-[10px] text-slate-400 font-bold uppercase">Database Connection</p>
                            <p class="text-sm font-extrabold text-slate-800" id="db_health_status">Checking...</p>
                            <p class="text-[11px] text-slate-500 mt-0.5" id="db_health_desc">-</p>
                        </div>
                    </div>
                    <!-- Cache Card -->
                    <div class="bg-white rounded-2xl p-4 border border-slate-200 flex items-center gap-3 shadow-3xs" id="cache_health_card">
                        <div class="p-2 bg-emerald-50 text-emerald-600 rounded-xl" id="cache_health_icon">
                            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 10V3L4 14h7v7l9-11h-7z"></path></svg>
                        </div>
                        <div>
                            <p class="text-[10px] text-slate-400 font-bold uppercase">Cache Driver Store</p>
                            <p class="text-sm font-extrabold text-slate-800" id="cache_health_status">Checking...</p>
                            <p class="text-[11px] text-slate-500 mt-0.5" id="cache_health_desc">-</p>
                        </div>
                    </div>
                    <!-- Stats Card -->
                    <div class="bg-white rounded-2xl p-4 border border-slate-200 flex items-center gap-3 shadow-3xs">
                        <div class="p-2 bg-slate-50 text-slate-600 rounded-xl">
                            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 3v2m6-2v2M9 19v2m6-2v2M5 9H3m2 6H3m18-6h-2m2 6h-2M7 19h10a2 2 0 002-2V7a2 2 0 00-2-2H7a2 2 0 00-2 2v10a2 2 0 002 2zM9 9h6v6H9V9z"></path></svg>
                        </div>
                        <div>
                            <p class="text-[10px] text-slate-400 font-bold uppercase">Server Metrics</p>
                            <p class="text-sm font-extrabold text-slate-800" id="system_health_env">-</p>
                            <p class="text-[11px] text-slate-500 mt-0.5" id="system_health_ram">-</p>
                        </div>
                    </div>
                </div>
            </div>

            <!-- Log Parser List Content -->
            <div class="p-6">
                <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-4 mb-4">
                    <div class="relative flex-grow max-w-md">
                        <span class="absolute inset-y-0 left-0 flex items-center pl-3 pointer-events-none text-slate-400">
                            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"></path></svg>
                        </span>
                        <input type="text" id="logSearchInput" oninput="filterLogs()" placeholder="Search error logs by keyword or class..." class="w-full pl-9 pr-4 py-2 text-xs rounded-xl border border-slate-300 focus:ring-2 focus:ring-indigo-500 outline-none transition-all placeholder:text-slate-400">
                    </div>
                    <p class="text-xs text-slate-500 font-medium" id="logSummaryLabel">Loading error logs...</p>
                </div>

                <!-- Logs Stream Console -->
                <div class="bg-slate-900 text-slate-100 rounded-2xl border border-slate-800 p-4 code-font text-xs max-h-[420px] overflow-y-auto space-y-3" id="log_stream_container">
                    <!-- Logs list generated dynamically -->
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
        // Global tab switching functionality
        function switchTab(tab) {
            const opBtn = document.getElementById('tabBtn-operations');
            const anBtn = document.getElementById('tabBtn-analytics');
            const opContent = document.getElementById('tabContent-operations');
            const anContent = document.getElementById('tabContent-analytics');

            if (!opBtn || !anBtn || !opContent || !anContent) return;

            if (tab === 'operations') {
                opBtn.classList.add('border-b-2', 'border-indigo-600', 'text-indigo-600');
                opBtn.classList.remove('border-transparent', 'text-slate-500');
                anBtn.classList.remove('border-b-2', 'border-indigo-600', 'text-indigo-600');
                anBtn.classList.add('border-transparent', 'text-slate-500');

                opContent.classList.remove('hidden');
                anContent.classList.add('hidden');
            } else {
                anBtn.classList.add('border-b-2', 'border-indigo-600', 'text-indigo-600');
                anBtn.classList.remove('border-transparent', 'text-slate-500');
                opBtn.classList.remove('border-b-2', 'border-indigo-600', 'text-indigo-600');
                opBtn.classList.add('border-transparent', 'text-slate-500');

                anContent.classList.remove('hidden');
                opContent.classList.add('hidden');
            }
        }

        document.addEventListener("DOMContentLoaded", function() {
            // Re-render / make sure correct default tab is set
            switchTab('operations');

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

            // 1. Sales & Orders Dual Chart
            const salesAndOrdersCtx = document.getElementById('salesAndOrdersChart').getContext('2d');
            new Chart(salesAndOrdersCtx, {
                type: 'bar',
                data: {
                    labels: labels,
                    datasets: [
                        {
                            label: 'Revenue (GH₵)',
                            data: values,
                            type: 'line',
                            borderColor: '#4f46e5',
                            backgroundColor: 'rgba(79, 70, 229, 0.05)',
                            borderWidth: 3,
                            tension: 0.3,
                            yAxisID: 'y'
                        },
                        {
                            label: 'Orders Processed',
                            data: salesData.map(item => item.order_count),
                            type: 'bar',
                            backgroundColor: 'rgba(16, 185, 129, 0.6)',
                            borderColor: '#10b981',
                            borderWidth: 1.5,
                            borderRadius: 6,
                            yAxisID: 'y1'
                        }
                    ]
                },
                options: {
                    responsive: true,
                    maintainAspectRatio: false,
                    plugins: {
                        legend: {
                            display: true,
                            position: 'top',
                            labels: {
                                font: {
                                    family: 'Plus Jakarta Sans',
                                    size: 10,
                                    weight: 'bold'
                                }
                            }
                        }
                    },
                    scales: {
                        y: {
                            type: 'linear',
                            display: true,
                            position: 'left',
                            beginAtZero: true,
                            title: {
                                display: true,
                                text: 'Revenue (GH₵)',
                                font: { family: 'Plus Jakarta Sans', size: 10, weight: 'bold' }
                            },
                            ticks: {
                                font: { family: 'JetBrains Mono', size: 9 }
                            }
                        },
                        y1: {
                            type: 'linear',
                            display: true,
                            position: 'right',
                            beginAtZero: true,
                            grid: {
                                drawOnChartArea: false
                            },
                            title: {
                                display: true,
                                text: 'Orders Count',
                                font: { family: 'Plus Jakarta Sans', size: 10, weight: 'bold' }
                            },
                            ticks: {
                                stepSize: 1,
                                font: { family: 'JetBrains Mono', size: 9 }
                            }
                        }
                    }
                }
            });

            // 2. Popular Food Items Doughnut Chart
            const topItemsData = @json($topPerformingItems);
            if (topItemsData.length > 0) {
                const popularCtx = document.getElementById('popularItemsChart').getContext('2d');
                const popularLabels = topItemsData.slice(0, 5).map(item => item.food_name);
                const popularValues = topItemsData.slice(0, 5).map(item => parseInt(item.total_quantity));
                
                new Chart(popularCtx, {
                    type: 'doughnut',
                    data: {
                        labels: popularLabels,
                        datasets: [{
                            data: popularValues,
                            backgroundColor: [
                                '#4f46e5',
                                '#10b981',
                                '#f59e0b',
                                '#ef4444',
                                '#8b5cf6'
                            ],
                            borderWidth: 2,
                            borderColor: '#ffffff'
                        }]
                    },
                    options: {
                        responsive: true,
                        maintainAspectRatio: false,
                        plugins: {
                            legend: {
                                display: true,
                                position: 'right',
                                labels: {
                                    font: {
                                        family: 'Plus Jakarta Sans',
                                        size: 10,
                                        weight: '600'
                                    },
                                    boxWidth: 12
                                }
                            }
                        },
                        cutout: '65%'
                    }
                });
            }

            // 3. Customer Feedback Radar Chart
            const totalFeedbackCount = {{ $metrics['rating_metrics']['total_feedback_count'] ?? 0 }};
            if (totalFeedbackCount > 0) {
                const feedbackCtx = document.getElementById('feedbackDimensionsChart').getContext('2d');
                new Chart(feedbackCtx, {
                    type: 'radar',
                    data: {
                        labels: ['Food Quality', 'Cleanliness', 'Service Speed', 'Value for Money'],
                        datasets: [{
                            label: 'Average Score',
                            data: [
                                {{ $metrics['rating_metrics']['average_food_quality'] ?? 0 }},
                                {{ $metrics['rating_metrics']['average_cleanliness'] ?? 0 }},
                                {{ $metrics['rating_metrics']['average_service_speed'] ?? 0 }},
                                {{ $metrics['rating_metrics']['average_price_value'] ?? 0 }}
                            ],
                            backgroundColor: 'rgba(79, 70, 229, 0.2)',
                            borderColor: '#4f46e5',
                            borderWidth: 2,
                            pointBackgroundColor: '#4f46e5',
                            pointHoverBackgroundColor: '#10b981'
                        }]
                    },
                    options: {
                        responsive: true,
                        maintainAspectRatio: false,
                        plugins: {
                            legend: {
                                display: false
                            }
                        },
                        scales: {
                            r: {
                                angleLines: {
                                    display: true
                                },
                                suggestedMin: 0,
                                suggestedMax: 5,
                                ticks: {
                                    stepSize: 1,
                                    font: { family: 'JetBrains Mono', size: 9 }
                                },
                                pointLabels: {
                                    font: {
                                        family: 'Plus Jakarta Sans',
                                        size: 10,
                                        weight: 'bold'
                                    }
                                }
                            }
                        }
                    }
                });
            }

            // 4. Fulfillment Status Breakdown Chart
            const statusBreakdown = @json($metrics['order_metrics']['status_breakdown']);
            const fulfillmentCtx = document.getElementById('fulfillmentStatusChart').getContext('2d');
            
            new Chart(fulfillmentCtx, {
                type: 'bar',
                data: {
                    labels: ['Pending', 'Preparing', 'Ready', 'Completed', 'Declined'],
                    datasets: [{
                        data: [
                            (statusBreakdown['PENDING'] || 0) + (statusBreakdown['ORDER_PLACED'] || 0),
                            statusBreakdown['PREPARING'] || 0,
                            statusBreakdown['READY'] || 0,
                            statusBreakdown['COMPLETED'] || 0,
                            statusBreakdown['DECLINED'] || 0
                        ],
                        backgroundColor: [
                            '#f59e0b',
                            '#3b82f6',
                            '#10b981',
                            '#64748b',
                            '#ef4444'
                        ],
                        borderRadius: 6,
                        borderWidth: 0
                    }]
                },
                options: {
                    indexAxis: 'y',
                    responsive: true,
                    maintainAspectRatio: false,
                    plugins: {
                        legend: {
                            display: false
                        }
                    },
                    scales: {
                        x: {
                            beginAtZero: true,
                            grid: {
                                color: 'rgba(241, 245, 249, 1)'
                            },
                            ticks: {
                                stepSize: 1,
                                font: { family: 'JetBrains Mono', size: 9 }
                            }
                        },
                        y: {
                            grid: {
                                display: false
                            },
                            ticks: {
                                font: {
                                    family: 'Plus Jakarta Sans',
                                    weight: 'bold',
                                    size: 10
                                }
                            }
                        }
                    }
                }
            });

            // 5. 30-Day Revenue Trend Line Chart
            const monthlySalesData = @json($monthlySales);
            const monthlyLabels = monthlySalesData.map(item => item.day);
            const monthlyValues = monthlySalesData.map(item => item.sales);
            
            const monthlyRevenueCtx = document.getElementById('monthlyRevenueTrendChart').getContext('2d');
            window.monthlyRevenueTrendChartInstance = new Chart(monthlyRevenueCtx, {
                type: 'line',
                data: {
                    labels: monthlyLabels,
                    datasets: [{
                        label: 'Sales Revenue (GH₵)',
                        data: monthlyValues,
                        borderColor: '#10b981', // emerald-500
                        backgroundColor: 'rgba(16, 185, 129, 0.05)',
                        borderWidth: 2.5,
                        pointBackgroundColor: '#10b981',
                        pointHoverBackgroundColor: '#4f46e5', // indigo-600
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
                                    return ` Daily Sales: GH₵ ${context.parsed.y.toFixed(2)}`;
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
                                    size: 9
                                },
                                maxRotation: 45,
                                minRotation: 45
                            }
                        }
                    }
                }
            });

            // Auto-refresh the incoming orders console every 10 seconds via background AJAX polling
            setInterval(() => {
                // Skip if the user is currently interacting with any select elements inside the console
                const activeEl = document.activeElement;
                if (activeEl && activeEl.tagName === 'SELECT' && activeEl.closest('#incoming-orders-console')) {
                    return;
                }
                
                fetch(window.location.href)
                    .then(response => response.text())
                    .then(html => {
                        const parser = new DOMParser();
                        const doc = parser.parseFromString(html, 'text/html');
                        const newConsole = doc.getElementById('incoming-orders-console');
                        const oldConsole = document.getElementById('incoming-orders-console');
                        
                        if (newConsole && oldConsole) {
                            oldConsole.innerHTML = newConsole.innerHTML;
                        }
                    })
                    .catch(err => console.warn('Order polling failed:', err));
            }, 10000);
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

    <!-- Inventory Thresholds Adjustment Modal -->
    <div id="inventory-modal" class="fixed inset-0 z-50 overflow-y-auto hidden" aria-labelledby="modal-title" role="dialog" aria-modal="true">
        <div class="flex items-end justify-center min-h-screen pt-4 px-4 pb-20 text-center sm:block sm:p-0">
            <!-- Background backdrop -->
            <div class="fixed inset-0 bg-slate-900 bg-opacity-75 transition-opacity" aria-hidden="true" onclick="closeInventoryModal()"></div>
            <!-- Center modal content -->
            <span class="hidden sm:inline-block sm:align-middle sm:h-screen" aria-hidden="true">&#8203;</span>
            <div class="inline-block align-bottom bg-white rounded-3xl text-left overflow-hidden shadow-2xl transform transition-all sm:my-8 sm:align-middle sm:max-w-md sm:w-full border border-slate-200">
                <div class="bg-gradient-to-r from-slate-900 via-indigo-950 to-slate-900 px-6 py-4 flex items-center justify-between text-white">
                    <div class="flex items-center gap-2">
                        <span class="text-xl">📦</span>
                        <div>
                            <h3 class="text-base font-bold" id="inventory-modal-title">Adjust Inventory Levels</h3>
                            <p class="text-[11px] text-slate-300">Update stock parameters and alerts</p>
                        </div>
                    </div>
                    <button type="button" onclick="closeInventoryModal()" class="text-slate-300 hover:text-white transition-colors outline-none focus:outline-none">
                        <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"></path>
                        </svg>
                    </button>
                </div>
                <form id="inventory-modal-form" method="POST" action="">
                    @csrf
                    <div class="p-6 bg-slate-50 space-y-4">
                        <div>
                            <label class="block text-xs font-bold text-slate-600 uppercase tracking-wider mb-1">Daily Prep Limit (Stock)</label>
                            <input type="number" id="modal-initial-stock" name="initial_stock" required min="1" class="w-full px-4 py-2.5 text-sm rounded-xl border border-slate-300 focus:ring-2 focus:ring-indigo-500 outline-none transition-all code-font">
                            <p class="text-[10px] text-slate-400 mt-1">Defines the starting daily stock level prepared for this food item.</p>
                        </div>
                        <div>
                            <label class="block text-xs font-bold text-slate-600 uppercase tracking-wider mb-1">Low Stock Alert Level</label>
                            <input type="number" id="modal-low-stock-threshold" name="low_stock_threshold" required min="0" class="w-full px-4 py-2.5 text-sm rounded-xl border border-slate-300 focus:ring-2 focus:ring-indigo-500 outline-none transition-all code-font">
                            <p class="text-[10px] text-slate-400 mt-1">Triggers low-stock alerts when remaining stock drops below this value.</p>
                        </div>
                    </div>
                    <div class="bg-slate-100 px-6 py-4 flex justify-end gap-3 border-t border-slate-200">
                        <button type="button" onclick="closeInventoryModal()" class="px-4 py-2 bg-slate-200 hover:bg-slate-300 text-slate-800 text-xs font-bold rounded-xl transition-all">
                            Cancel
                        </button>
                        <button type="submit" class="px-4 py-2 bg-indigo-600 hover:bg-indigo-700 text-white text-xs font-bold rounded-xl transition-all shadow-xs">
                            Save Settings
                        </button>
                    </div>
                </form>
            </div>
        </div>
    </div>

    <!-- JavaScript Helpers for Modals & PWA Notifications -->
    <script>
        let qrCodeInstance = null;
        let qrCodeLink = "";

        function openInventoryModal(id, name, initialStock, lowStockThreshold) {
            document.getElementById('inventory-modal-title').textContent = `Adjust Inventory: ${name}`;
            document.getElementById('inventory-modal-form').action = `/api/vendor/food-items/${id}/update-inventory`;
            document.getElementById('modal-initial-stock').value = initialStock;
            document.getElementById('modal-low-stock-threshold').value = lowStockThreshold;
            document.getElementById('inventory-modal').classList.remove('hidden');
        }

        function closeInventoryModal() {
            document.getElementById('inventory-modal').classList.add('hidden');
        }

        function applyChartDateRange() {
            const startDate = document.getElementById('chart-start-date').value;
            const endDate = document.getElementById('chart-end-date').value;

            if (!startDate || !endDate) {
                alert('Please select both start and end dates.');
                return;
            }

            // Show a loading style or spin the refresh button icon
            const btn = document.querySelector('button[onclick="applyChartDateRange()"]');
            const svg = btn ? btn.querySelector('svg') : null;
            if (svg) svg.classList.add('animate-spin');

            const url = `/api/vendor/analytics/trends?start_date=${startDate}&end_date=${endDate}`;

            fetch(url, {
                headers: {
                    'Accept': 'application/json'
                }
            })
            .then(res => {
                if (!res.ok) {
                    throw new Error('Network response was not OK');
                }
                return res.json();
            })
            .then(data => {
                if (data.success) {
                    // Update monthlyRevenueTrendChartInstance with new labels and datasets!
                    if (window.monthlyRevenueTrendChartInstance) {
                        window.monthlyRevenueTrendChartInstance.data.labels = data.labels;
                        window.monthlyRevenueTrendChartInstance.data.datasets[0].data = data.sales;
                        window.monthlyRevenueTrendChartInstance.update();
                    }
                    console.log('Successfully updated trend charts via API.');
                } else {
                    alert('Error updating trends: ' + (data.message || 'Unknown error'));
                }
            })
            .catch(err => {
                console.error('Error fetching trends:', err);
                alert('Error fetching dynamic trends. Please verify connectivity.');
            })
            .finally(() => {
                if (svg) svg.classList.remove('animate-spin');
            });
        }

        function recalculateStaffingInsights() {
            const multiplier = parseFloat(document.getElementById('campus-traffic-modifier').value);
            
            // Scale elements based on activity level
            const breakfastTraffic = Math.round(15 * multiplier);
            const lunchTraffic = Math.round(45 * multiplier);
            const afternoonTraffic = Math.round(8 * multiplier);
            
            document.getElementById('traffic-breakfast').innerText = `Moderate (${breakfastTraffic} ord/hr)`;
            document.getElementById('traffic-lunch').innerText = lunchTraffic > 50 ? `CRITICAL (${lunchTraffic} ord/hr)` : `High (${lunchTraffic} ord/hr)`;
            document.getElementById('traffic-afternoon').innerText = `Light (${afternoonTraffic} ord/hr)`;
            
            // Recalculate staffing levels
            const breakfastCooks = Math.max(1, Math.round(2 * multiplier));
            const breakfastCashiers = Math.max(1, Math.round(1 * multiplier));
            
            const lunchCooks = Math.max(1, Math.round(3 * multiplier));
            const lunchCashiers = Math.max(1, Math.round(2 * multiplier));
            
            const afternoonCooks = Math.max(1, Math.round(1 * multiplier));
            const afternoonCashiers = Math.max(1, Math.round(1 * multiplier));
            
            document.getElementById('cooks-breakfast').innerText = `${breakfastCooks} ${breakfastCooks === 1 ? 'Chef' : 'Chefs'}`;
            document.getElementById('cashiers-breakfast').innerText = `${breakfastCashiers} ${breakfastCashiers === 1 ? 'Clerk' : 'Clerks'}`;
            
            document.getElementById('cooks-lunch').innerText = `${lunchCooks} ${lunchCooks === 1 ? 'Chef' : 'Chefs'}`;
            document.getElementById('cashiers-lunch').innerText = `${lunchCashiers} ${lunchCashiers === 1 ? 'Clerk' : 'Clerks'}`;
            
            document.getElementById('cooks-afternoon').innerText = `${afternoonCooks} ${afternoonCooks === 1 ? 'Chef' : 'Chefs'}`;
            document.getElementById('cashiers-afternoon').innerText = `${afternoonCashiers} ${afternoonCashiers === 1 ? 'Clerk' : 'Clerks'}`;
            
            // Scale master boxes
            const totalStaff = lunchCooks + lunchCashiers;
            document.getElementById('optimal-staff-count').innerText = `${totalStaff} Cooks / Cashiers`;
            document.getElementById('staff-utilization-rating').innerText = `Capacity utilization: ${Math.round(88 * multiplier)}%`;
            
            const jollofPortions = Math.round(35 * multiplier);
            const waakyePortions = Math.round(15 * multiplier);
            const drinksPortions = Math.round(12 * multiplier);
            document.getElementById('recommended-prep-qty').innerText = `${jollofPortions} Portions Jollof`;
            document.getElementById('recommended-prep-details').innerText = `Prepare ${waakyePortions} Portions Waakye, ${drinksPortions} Drinks`;
            document.getElementById('peak-hours-volume').innerText = `Estimated lunch rush: ${lunchTraffic}+ orders/hour`;
        }

        // Touch gesture state tracking for swipe-to-complete
        const swipeData = {};

        function handleTouchStart(e, id) {
            const touch = e.touches[0];
            swipeData[id] = {
                startX: touch.clientX,
                startY: touch.clientY,
                card: document.getElementById('order-card-' + id),
                bg: document.getElementById('swipe-bg-' + id),
                triggered: false
            };
            if (swipeData[id].card) {
                swipeData[id].card.classList.remove('transition-transform', 'duration-150');
            }
        }

        function handleTouchMove(e, id) {
            const data = swipeData[id];
            if (!data || !data.card || data.triggered) return;

            const touch = e.touches[0];
            const diffX = touch.clientX - data.startX;
            const diffY = touch.clientY - data.startY;

            // If vertical scroll is larger, ignore horizontal swipe
            if (Math.abs(diffY) > Math.abs(diffX) && Math.abs(diffX) < 10) {
                return;
            }

            // Only allow swiping to the right (positive diffX)
            if (diffX > 0) {
                e.preventDefault(); // prevent vertical scrolling when swiping horizontally
                const moveAmount = Math.min(diffX, 220); // Cap the movement
                data.card.style.transform = `translateX(${moveAmount}px)`;
                
                if (data.bg) {
                    data.bg.style.opacity = Math.min(1, moveAmount / 120);
                }
            }
        }

        function handleTouchEnd(e, id) {
            const data = swipeData[id];
            if (!data || !data.card) return;

            data.card.classList.add('transition-transform', 'duration-150');
            const touch = e.changedTouches[0];
            const diffX = touch.clientX - data.startX;

            if (diffX > 140) {
                // Swipe triggered successfully!
                data.triggered = true;
                data.card.style.transform = 'translateX(100%)';
                if (data.bg) {
                    data.bg.style.opacity = '1';
                    const label = data.bg.querySelector('span:last-child');
                    if (label) label.textContent = '⚡ Completing...';
                }
                
                // Programmatically submit the form to update state to COMPLETED
                setTimeout(() => {
                    const form = document.getElementById('status-form-' + id);
                    if (form) {
                        const select = form.querySelector('select[name="status"]');
                        if (select) {
                            select.value = 'COMPLETED';
                            form.submit();
                        }
                    } else {
                        window.location.reload();
                    }
                }, 300);
            } else {
                // Reset card position
                data.card.style.transform = 'translateX(0px)';
                if (data.bg) {
                    data.bg.style.opacity = '0';
                }
            }
            
            delete swipeData[id];
        }

        // Low stock toast alerts function
        function showLowStockToast(items) {
            const container = document.getElementById('lowStockToastContainer');
            if (!container) return;
            
            items.forEach((item, index) => {
                setTimeout(() => {
                    const toast = document.createElement('div');
                    toast.className = "pointer-events-auto bg-slate-950 text-white rounded-2xl shadow-2xl border border-slate-800 p-4 flex gap-3 transition-all transform translate-x-full duration-300 relative overflow-hidden";
                    toast.style.borderLeft = "4px solid #ef4444"; // red-500
                    
                    toast.innerHTML = `
                        <div class="text-red-500 text-xl">⚠️</div>
                        <div class="flex-grow">
                            <h5 class="text-xs font-extrabold text-slate-100 uppercase tracking-wider">Low Stock Alert</h5>
                            <p class="text-sm font-bold text-white mt-0.5">${item.name}</p>
                            <p class="text-[11px] text-slate-400 mt-1">Remaining: <span class="text-red-400 font-bold">${item.remaining}</span> / ${item.initial}</p>
                        </div>
                        <button type="button" onclick="this.parentElement.remove()" class="text-slate-400 hover:text-white transition-colors self-start">
                            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"></path>
                            </svg>
                        </button>
                    `;
                    
                    container.appendChild(toast);
                    
                    // Slide in
                    setTimeout(() => {
                        toast.classList.remove('translate-x-full');
                    }, 50);
                    
                    // Auto remove after 8 seconds
                    setTimeout(() => {
                        toast.classList.add('opacity-0', 'scale-95');
                        setTimeout(() => toast.remove(), 300);
                    }, 8000 + (index * 1500));
                }, index * 300);
            });
        }

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

            // Trigger low-stock toast alerts
            @if(!empty($lowStockItems))
                showLowStockToast(@json($lowStockItems));
            @endif

            // Initialize System Health and Diagnostic Log Streams
            fetchSystemHealth();
            fetchLogs();
            loadSentimentReport();
        });

        // Diagnostics Log & Health Console routines
        let activeLogsList = [];

        function fetchSystemHealth() {
            const dbCard = document.getElementById('db_health_card');
            const cacheCard = document.getElementById('cache_health_card');
            
            document.getElementById('db_health_status').textContent = "Checking...";
            document.getElementById('cache_health_status').textContent = "Checking...";
            
            fetch('/api/system/status')
            .then(res => {
                if (!res.ok && res.status !== 503) {
                    throw new Error("HTTP " + res.status);
                }
                return res.json();
            })
            .then(data => {
                const db = data.services.database;
                const cache = data.services.cache;
                
                // DB Card styling
                const dbStatusEl = document.getElementById('db_health_status');
                dbStatusEl.textContent = db.status;
                document.getElementById('db_health_desc').textContent = db.driver.toUpperCase() + ": " + (db.status === 'OK' ? 'Connected successfully' : 'Connection failed');
                
                if (db.status === 'OK') {
                    dbStatusEl.className = "text-sm font-extrabold text-emerald-600";
                    dbCard.className = "bg-white rounded-2xl p-4 border border-slate-200 flex items-center gap-3 shadow-3xs";
                } else {
                    dbStatusEl.className = "text-sm font-extrabold text-rose-600";
                    dbCard.className = "bg-rose-50 rounded-2xl p-4 border border-rose-200 flex items-center gap-3 shadow-3xs animate-pulse";
                }
                
                // Cache Card styling
                const cacheStatusEl = document.getElementById('cache_health_status');
                cacheStatusEl.textContent = cache.status;
                document.getElementById('cache_health_desc').textContent = cache.driver.toUpperCase() + ": " + (cache.status === 'OK' ? 'Active storage store' : 'Store unreachable');
                
                if (cache.status === 'OK') {
                    cacheStatusEl.className = "text-sm font-extrabold text-emerald-600";
                    cacheCard.className = "bg-white rounded-2xl p-4 border border-slate-200 flex items-center gap-3 shadow-3xs";
                } else {
                    cacheStatusEl.className = "text-sm font-extrabold text-rose-600";
                    cacheCard.className = "bg-rose-50 rounded-2xl p-4 border border-rose-200 flex items-center gap-3 shadow-3xs animate-pulse";
                }
                
                // System stats styling
                document.getElementById('system_health_env').textContent = `PHP ${data.php_version} (${data.environment.toUpperCase()})`;
                document.getElementById('system_health_ram').textContent = `RAM: ${data.diagnostics.memory_usage_mb} MB | Disk: ${data.diagnostics.disk_free_space_gb}`;
            })
            .catch(err => {
                console.error("Health diagnostics failed:", err);
                document.getElementById('db_health_status').textContent = "ERROR";
                document.getElementById('db_health_desc').textContent = "Failed to query system health API";
                document.getElementById('cache_health_status').textContent = "ERROR";
                document.getElementById('cache_health_desc').textContent = "Failed to query system health API";
            });
        }

        function fetchLogs() {
            const container = document.getElementById('log_stream_container');
            container.innerHTML = `<div class="py-12 text-center text-slate-400 animate-pulse">⚡ Connecting log streaming buffer...</div>`;
            
            fetch('/api/system/logs')
            .then(res => res.json())
            .then(data => {
                if (data.success) {
                    activeLogsList = data.logs || [];
                    renderLogs(activeLogsList);
                } else {
                    container.innerHTML = `<div class="py-8 text-center text-rose-400 font-bold">❌ Failed to parse system logs: ${data.message}</div>`;
                }
            })
            .catch(err => {
                console.error("Log fetch failed:", err);
                container.innerHTML = `<div class="py-8 text-center text-rose-400 font-bold">❌ Server connection failure during log streaming</div>`;
            });
        }

        function renderLogs(logs) {
            const container = document.getElementById('log_stream_container');
            document.getElementById('logSummaryLabel').textContent = `Showing latest ${logs.length} parsed log entries`;
            
            if (logs.length === 0) {
                container.innerHTML = `<div class="py-12 text-center text-slate-500">✨ Log buffer is empty. No recent errors or exceptions captured.</div>`;
                return;
            }
            
            let html = "";
            logs.forEach((log, index) => {
                let badgeClass = "bg-slate-800 text-slate-300";
                if (log.level === 'ERROR' || log.level === 'CRITICAL' || log.level === 'EMERGENCY' || log.level === 'ALERT') {
                    badgeClass = "bg-red-500/20 text-red-400 border border-red-500/30";
                } else if (log.level === 'WARNING') {
                    badgeClass = "bg-amber-500/20 text-amber-400 border border-amber-500/30";
                } else if (log.level === 'INFO' || log.level === 'DEBUG') {
                    badgeClass = "bg-sky-500/20 text-sky-400 border border-sky-500/30";
                }
                
                html += `
                    <div class="border-b border-slate-800/80 pb-3 last:border-none last:pb-0">
                        <div class="flex flex-col md:flex-row md:items-center justify-between gap-2">
                            <div class="flex items-center gap-2 flex-wrap">
                                <span class="px-2 py-0.5 text-[10px] rounded-md font-extrabold ${badgeClass}">${log.level}</span>
                                <span class="text-slate-500 text-[11px] font-bold">${log.timestamp}</span>
                                <span class="text-slate-400 text-[10px] uppercase font-bold tracking-wider">${log.environment}</span>
                            </div>
                            ${log.has_stack ? `
                                <button onclick="toggleStackTrace(${index})" class="text-[10px] text-indigo-400 hover:text-indigo-300 font-bold underline focus:outline-none">
                                    Show Details/Stack
                                </button>
                            ` : ''}
                        </div>
                        <p class="text-slate-200 mt-1.5 leading-relaxed break-all font-medium">${escapeHtml(log.short_message)}</p>
                        
                        ${log.has_stack ? `
                            <div id="stack-trace-${index}" class="hidden mt-3 p-3 bg-slate-950 text-slate-400 rounded-xl border border-slate-800/50 max-h-[300px] overflow-y-auto whitespace-pre-wrap select-text leading-relaxed font-mono">
${escapeHtml(log.full_message)}
                            </div>
                        ` : ''}
                    </div>
                `;
            });
            
            container.innerHTML = html;
        }

        function toggleStackTrace(index) {
            const el = document.getElementById(`stack-trace-${index}`);
            if (el) {
                el.classList.toggle('hidden');
            }
        }

        function filterLogs() {
            const query = document.getElementById('logSearchInput').value.toLowerCase().trim();
            if (!query) {
                renderLogs(activeLogsList);
                return;
            }
            
            const filtered = activeLogsList.filter(log => {
                return log.full_message.toLowerCase().includes(query) || 
                       log.level.toLowerCase().includes(query) || 
                       log.timestamp.includes(query);
            });
            
            renderLogs(filtered);
        }

        function clearLogs() {
            if (!confirm("Are you sure you want to permanently empty the Laravel error log file? This action is irreversible.")) {
                return;
            }
            
            fetch('/api/system/logs/clear', {
                method: 'POST',
                headers: {
                    'X-CSRF-TOKEN': '{{ csrf_token() }}'
                }
            })
            .then(res => res.json())
            .then(data => {
                if (data.success) {
                    fetchLogs();
                    alert("Laravel logs truncated successfully!");
                } else {
                    alert("Failed to clear logs: " + data.message);
                }
            })
            .catch(err => {
                console.error("Clear logs request failed:", err);
                alert("Network communication failure while trying to clear logs.");
            });
        }

        function loadSentimentReport() {
            const container = document.getElementById('sentiment-report-content');
            const icon = document.getElementById('sentiment-refresh-icon');
            if (icon) icon.classList.add('animate-spin');

            fetch('/api/vendor/feedback/sentiment', {
                headers: {
                    'Accept': 'application/json',
                    'X-Requested-With': 'XMLHttpRequest'
                }
            })
            .then(res => res.json())
            .then(data => {
                if (icon) icon.classList.remove('animate-spin');
                if (data.success && data.report) {
                    container.innerHTML = formatMarkdownToHtml(data.report);
                } else {
                    container.innerHTML = `<div class="p-4 bg-rose-50 text-rose-700 rounded-2xl text-xs font-semibold">Failed to load cognitive sentiment analysis. ${data.message || ''}</div>`;
                }
            })
            .catch(err => {
                if (icon) icon.classList.remove('animate-spin');
                container.innerHTML = `<div class="p-4 bg-rose-50 text-rose-700 rounded-2xl text-xs font-semibold">Failed to connect to the sentiment analytical engine.</div>`;
            });
        }

        function formatMarkdownToHtml(md) {
            if (!md) return '';
            let html = md;
            
            // Convert headers
            html = html.replace(/^# (.*$)/gim, '<h1 class="text-base font-black text-slate-900 mb-4 border-b pb-2 flex items-center gap-1.5">$1</h1>');
            html = html.replace(/^## (.*$)/gim, '<h2 class="text-sm font-extrabold text-indigo-950 mt-4 mb-2 flex items-center gap-1">$1</h2>');
            html = html.replace(/^### (.*$)/gim, '<h3 class="text-xs font-bold text-indigo-800 mt-3 mb-1.5">$1</h3>');
            
            // Convert bold text
            html = html.replace(/\*\*(.*?)\*\*/g, '<strong class="font-extrabold text-slate-900">$1</strong>');
            
            // Convert bullet points
            html = html.replace(/^\* (.*$)/gim, '<li class="ml-4 list-disc text-slate-600 mt-1">$1</li>');
            html = html.replace(/^- (.*$)/gim, '<li class="ml-4 list-disc text-slate-600 mt-1">$1</li>');
            
            // Replace newlines with paragraph spacing
            html = html.split('\n\n').map(p => {
                if (p.trim().startsWith('<li') || p.trim().startsWith('<h')) return p;
                return `<p class="mt-2 text-slate-600">${p}</p>`;
            }).join('');

            return html;
        }

        function escapeHtml(str) {
            if (!str) return '';
            return str
                .replace(/&/g, "&amp;")
                .replace(/</g, "&lt;")
                .replace(/>/g, "&gt;")
                .replace(/"/g, "&quot;")
                .replace(/'/g, "&#039;");
        }
        });
    </script>

    <!-- Interactive QR Code Claim Scanner Modal -->
    <div id="scannerModal" class="fixed inset-0 bg-slate-900/60 backdrop-blur-sm hidden flex items-center justify-center z-50 p-4 transition-all duration-300">
        <div class="bg-white rounded-3xl border border-slate-200 shadow-2xl max-w-lg w-full overflow-hidden flex flex-col transform scale-95 transition-all duration-300" id="scannerModalContent">
            <!-- Modal Header -->
            <div class="px-6 py-5 bg-gradient-to-r from-indigo-600 to-purple-600 text-white flex items-center justify-between">
                <div class="flex items-center gap-2.5">
                    <div class="p-2 bg-white/10 rounded-xl">
                        <svg class="w-5 h-5 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v1m6 11h2m-6 0h-2v4m0-11v3m0 0h.01M12 12h.01M16 20h2M4 12h4m12 0a8 8 0 11-16 0 8 8 0 0116 0z"></path>
                        </svg>
                    </div>
                    <div>
                        <h3 class="text-base font-extrabold tracking-tight">Student Claim Ticket Scanner</h3>
                        <p class="text-[11px] text-indigo-100 font-medium">Verify student custody hand-offs instantly</p>
                    </div>
                </div>
                <button onclick="closeScannerModal()" class="p-1 text-white/80 hover:text-white rounded-lg hover:bg-white/10 transition-colors">
                    <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"></path>
                    </svg>
                </button>
            </div>

            <!-- Modal Content -->
            <div class="p-6 flex flex-col gap-5 overflow-y-auto max-h-[75vh]">
                <!-- Camera view area -->
                <div class="flex flex-col items-center justify-center bg-slate-50 border border-slate-200 rounded-2xl p-4 relative overflow-hidden min-h-[250px]">
                    <div id="reader" class="w-full max-w-[360px] rounded-xl overflow-hidden shadow-sm" style="background: #000;"></div>
                    <div id="scanner-loader" class="absolute inset-0 bg-slate-900/70 flex flex-col items-center justify-center text-white text-xs gap-3 font-semibold">
                        <svg class="w-8 h-8 animate-spin text-indigo-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                            <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                        </svg>
                        <span>Starting Digital Camera Scanner...</span>
                    </div>
                </div>

                <!-- Live Status Banner -->
                <div id="scanner-status" class="p-3.5 bg-indigo-50 border border-indigo-100 text-indigo-900 text-xs font-bold rounded-xl text-center leading-relaxed flex items-center justify-center gap-2">
                    <span>💡 Tip: Align student's Claim QR code ticket inside the green square target bounds.</span>
                </div>

                <!-- Fallback / Manual Verification Form -->
                <div class="border-t border-slate-100 pt-4">
                    <h4 class="text-xs font-black text-slate-400 uppercase tracking-wider mb-3">Or Manual PIN Verification</h4>
                    <form id="manual-verify-form" onsubmit="handleManualVerify(event)" class="grid grid-cols-2 gap-3">
                        <div class="flex flex-col gap-1 col-span-2 sm:col-span-1">
                            <label class="text-[10px] font-bold text-slate-500 uppercase">Order ID / Ticket #</label>
                            <input type="number" id="verify-order-id" placeholder="e.g. 1005" required class="px-3.5 py-2 border border-slate-200 rounded-xl text-xs font-bold focus:outline-indigo-600">
                        </div>
                        <div class="flex flex-col gap-1 col-span-2 sm:col-span-1">
                            <label class="text-[10px] font-bold text-slate-500 uppercase">Secure Hand-off PIN</label>
                            <input type="text" id="verify-pin" placeholder="e.g. A92C" required class="px-3.5 py-2 border border-slate-200 rounded-xl text-xs font-bold focus:outline-indigo-600">
                        </div>
                        <button type="submit" class="col-span-2 py-2.5 px-4 bg-slate-900 hover:bg-slate-800 text-white font-extrabold text-xs rounded-xl transition-all shadow flex items-center justify-center gap-2 mt-1">
                            Verify & Release Pre-Order
                        </button>
                    </form>
                </div>
            </div>
        </div>
    </div>

    <!-- QR Code Scan JS implementation -->
    <script type="text/javascript">
        let html5QrcodeScanner = null;

        function openScannerModal() {
            const modal = document.getElementById('scannerModal');
            const modalContent = document.getElementById('scannerModalContent');
            const loader = document.getElementById('scanner-loader');
            const status = document.getElementById('scanner-status');
            
            modal.classList.remove('hidden');
            setTimeout(() => {
                modalContent.classList.remove('scale-95');
                modalContent.classList.add('scale-100');
            }, 50);

            // Reset manual form fields
            document.getElementById('manual-verify-form').reset();
            
            // Setup state
            loader.style.display = 'flex';
            status.className = "p-3.5 bg-indigo-50 border border-indigo-100 text-indigo-900 text-xs font-bold rounded-xl text-center leading-relaxed flex items-center justify-center gap-2";
            status.innerHTML = "<span>💡 Tip: Align student's Claim QR code ticket inside the camera viewport.</span>";

            // Initialize Html5Qrcode
            html5QrcodeScanner = new Html5Qrcode("reader");
            
            // Start scanner with back camera (environment) preferred
            html5QrcodeScanner.start(
                { facingMode: "environment" },
                {
                    fps: 10,
                    qrbox: { width: 220, height: 220 }
                },
                onScanSuccess,
                onScanError
            )
            .then(() => {
                loader.style.display = 'none';
            })
            .catch(err => {
                console.error("Camera scanner startup failed:", err);
                loader.style.display = 'none';
                status.className = "p-3.5 bg-amber-50 border border-amber-200 text-amber-800 text-xs font-bold rounded-xl text-center leading-relaxed";
                status.innerHTML = "⚠️ Camera permission denied or not found. Please use the manual PIN verification panel below.";
            });
        }

        function closeScannerModal() {
            const modal = document.getElementById('scannerModal');
            const modalContent = document.getElementById('scannerModalContent');
            
            modalContent.classList.remove('scale-100');
            modalContent.classList.add('scale-95');
            
            if (html5QrcodeScanner) {
                html5QrcodeScanner.stop().then(() => {
                    html5QrcodeScanner.clear();
                    html5QrcodeScanner = null;
                    modal.classList.add('hidden');
                }).catch(err => {
                    console.error("Failed to stop scanner cleanly", err);
                    html5QrcodeScanner = null;
                    modal.classList.add('hidden');
                });
            } else {
                modal.classList.add('hidden');
            }
        }

        function onScanSuccess(decodedText, decodedResult) {
            console.log("QR Code Scanned successfully:", decodedText);
            
            // Play scan beep sound
            playScanBeep();
            
            const status = document.getElementById('scanner-status');
            status.className = "p-3.5 bg-blue-50 border border-blue-100 text-blue-900 text-xs font-bold rounded-xl text-center leading-relaxed flex items-center justify-center gap-2";
            status.innerHTML = "🔄 Validating claiming ticket credentials...";

            let orderId = null;
            let pin = null;

            // Extract order ID
            const orderMatch = decodedText.match(/ATU-TKT-(\d+)/);
            if (orderMatch) {
                orderId = parseInt(orderMatch[1]);
            }

            // Extract PIN
            const pinMatch = decodedText.match(/PIN:\s*([A-Za-z0-9]+)/i);
            if (pinMatch) {
                pin = pinMatch[1].trim();
            }

            if (orderId && pin) {
                triggerPickupVerification(orderId, pin);
            } else {
                status.className = "p-3.5 bg-rose-50 border border-rose-200 text-rose-800 text-xs font-bold rounded-xl text-center leading-relaxed";
                status.innerHTML = "❌ Invalid ATU Claim QR Code format. Try scanning again or input PIN manually.";
            }
        }

        function onScanError(errorMessage) {
            // Simple log to keep console clean
        }

        function handleManualVerify(event) {
            event.preventDefault();
            const orderId = document.getElementById('verify-order-id').value;
            const pin = document.getElementById('verify-pin').value;
            
            const status = document.getElementById('scanner-status');
            status.className = "p-3.5 bg-blue-50 border border-blue-100 text-blue-900 text-xs font-bold rounded-xl text-center leading-relaxed flex items-center justify-center gap-2";
            status.innerHTML = "🔄 Validating manual PIN credentials...";

            triggerPickupVerification(orderId, pin);
        }

        function triggerPickupVerification(orderId, pin) {
            const status = document.getElementById('scanner-status');
            const vendorId = "{{ $vendor->id }}";

            fetch(`/api/orders/${orderId}/verify-pickup`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Accept': 'application/json',
                    'X-Requested-With': 'XMLHttpRequest',
                    'X-CSRF-TOKEN': '{{ csrf_token() }}'
                },
                body: JSON.stringify({
                    vendor_id: vendorId,
                    pickup_pin: pin
                })
            })
            .then(res => res.json())
            .then(data => {
                if (data.success || (data.message && data.message.toLowerCase().includes("success"))) {
                    status.className = "p-3.5 bg-emerald-50 border border-emerald-200 text-emerald-800 text-xs font-bold rounded-xl text-center leading-relaxed";
                    status.innerHTML = "🎉 Order Claim Verified! Food released. Status updated to Completed.";
                    
                    // Play success sound
                    playSuccessSound();

                    // Reload view to update status
                    setTimeout(() => {
                        closeScannerModal();
                        window.location.reload();
                    }, 2000);
                } else {
                    status.className = "p-3.5 bg-rose-50 border border-rose-200 text-rose-800 text-xs font-bold rounded-xl text-center leading-relaxed";
                    status.innerHTML = `❌ Verification Failed: ${data.message || 'Incorrect pickup PIN.'}`;
                }
            })
            .catch(err => {
                console.error("Verification failed:", err);
                status.className = "p-3.5 bg-rose-50 border border-rose-200 text-rose-800 text-xs font-bold rounded-xl text-center leading-relaxed";
                status.innerHTML = "❌ Network connection error. Verification could not be finalized.";
            });
        }

        function playScanBeep() {
            try {
                const ctx = new (window.AudioContext || window.webkitAudioContext)();
                const osc = ctx.createOscillator();
                const gain = ctx.createGain();
                osc.type = "sine";
                osc.frequency.setValueAtTime(1000, ctx.currentTime);
                gain.gain.setValueAtTime(0.3, ctx.currentTime);
                gain.gain.exponentialRampToValueAtTime(0.01, ctx.currentTime + 0.15);
                osc.connect(gain);
                gain.connect(ctx.destination);
                osc.start();
                osc.stop(ctx.currentTime + 0.15);
            } catch (e) {
                console.log("Audio feedback ignored", e);
            }
        }

        function playSuccessSound() {
            try {
                const ctx = new (window.AudioContext || window.webkitAudioContext)();
                const osc = ctx.createOscillator();
                const gain = ctx.createGain();
                osc.type = "sine";
                osc.frequency.setValueAtTime(523.25, ctx.currentTime); // C5
                osc.frequency.setValueAtTime(659.25, ctx.currentTime + 0.1); // E5
                osc.frequency.setValueAtTime(783.99, ctx.currentTime + 0.2); // G5
                gain.gain.setValueAtTime(0.3, ctx.currentTime);
                gain.gain.exponentialRampToValueAtTime(0.01, ctx.currentTime + 0.4);
                osc.connect(gain);
                gain.connect(ctx.destination);
                osc.start();
                osc.stop(ctx.currentTime + 0.4);
            } catch (e) {
                console.log("Audio feedback ignored", e);
            }
        }
    </script>

</body>
</html>
