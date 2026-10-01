import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { App } from './app';

@Component({
  selector: 'app-overview-page',
  imports: [CommonModule, FormsModule],
  templateUrl: './overview-page.component.html',
  styleUrl: './app.scss',
})
export class OverviewPageComponent extends App {}
