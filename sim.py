import tkinter as tk
import time

#thingy1 dimensions 50x50

WIDTH = 1200
HEIGHT = 1000


class thingy1(object):
    def __init__(self, canvas, vx, vy, spawnx, spawny):
        self.canvas = canvas
        self.id = canvas.create_rectangle(spawnx-25, spawny+25, spawnx+25, spawny-25, fill="red")
        self.vx = vx
        self.vy = vy
        self.x = spawnx
        self.y = spawny

    def move(self):
        x1, y1, x2, y2 = self.canvas.bbox(self.id)
        if x2 > WIDTH:
            self.vx *= -1
        if x1 < 0:
            self.vx *= -1
        if y2 > HEIGHT:
            self.vy *= -1
        if y1 < 0:
            self.vy *= -1
        self.canvas.move(self.id,self.vx,self.vy)
        

class App(object):
    def __init__(self,master):
        self.master = master
        self.canvas = tk.Canvas(root, width = WIDTH, height = HEIGHT)
        self.canvas.pack()

        self.me = self.canvas.create_rectangle(550,400,650,600, fill="blue")

        self.things = [thingy1(self.canvas, 1,1,300,300)]
        self.canvas.pack()
        self.master.after(0,self.animation)

    def animation(self):
        for thing in self.things:
            thing.move()
        self.master.after(12,self.animation)

    
if __name__ == "__main__":
    root = tk.Tk()
    canvas = App(root)
    root.mainloop()
