import { Routes } from '@angular/router';

export const routes: Routes = [
  // ---- Mother mode (default) ----
  {
    path: '',
    loadComponent: () => import('./pages/mother-home.page').then(m => m.MotherHomePage),
    title: 'Kundli · नमस्ते माँ',
  },
  {
    path: 'new-match',
    loadComponent: () => import('./pages/new-match.page').then(m => m.NewMatchPage),
    title: 'Kundli · नया मिलान',
  },
  {
    path: 'confirm',
    loadComponent: () => import('./pages/confirm.page').then(m => m.ConfirmPage),
    title: 'Kundli · पुष्टि',
  },
  {
    path: 'result/:id',
    loadComponent: () => import('./pages/mother-result.page').then(m => m.MotherResultPage),
    title: 'Kundli · परिणाम',
  },
  {
    path: 'history',
    loadComponent: () => import('./pages/history.page').then(m => m.HistoryPage),
    title: 'Kundli · पिछले मिलान',
  },
  {
    path: 'print/:id',
    loadComponent: () => import('./pages/print-report.page').then(m => m.PrintReportPage),
    title: 'Kundli · Print report',
  },

  // ---- Advanced mode: the pre-M5 pages, moved under a prefix ----
  {
    path: 'advanced',
    loadComponent: () => import('./pages/charts.page').then(m => m.ChartsPage),
    title: 'Kundli · Birth Charts',
  },
  {
    path: 'advanced/match',
    loadComponent: () => import('./pages/match.page').then(m => m.MatchPage),
    title: 'Kundli · Match',
  },

  { path: '**', redirectTo: '' },
];
