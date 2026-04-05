import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class SearchService {
  private querySubject = new BehaviorSubject<string>('');
  query$ = this.querySubject.asObservable();

  private pageSubject = new BehaviorSubject<string>('');
  page$ = this.pageSubject.asObservable();

  setQuery(query: string, page: string) {
    this.pageSubject.next(page);
    this.querySubject.next(query);
  }

  clearQuery() {
    this.querySubject.next('');
  }
}
