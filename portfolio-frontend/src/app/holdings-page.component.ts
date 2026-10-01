import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { App } from './app';

@Component({
  selector: 'app-holdings-page',
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './holdings-page.component.html',
  styleUrl: './app.scss',
})
export class HoldingsPageComponent extends App {}
