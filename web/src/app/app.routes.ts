import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: '',
    loadComponent: () => import('./pages/charts.page').then(m => m.ChartsPage),
    title: 'Kundli · Birth Charts',
  },
  {
    path: 'match',
    loadComponent: () => import('./pages/match.page').then(m => m.MatchPage),
    title: 'Kundli · Match',
  },
  { path: '**', redirectTo: '' },
];
