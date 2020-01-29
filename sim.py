import tkinter as tk
import time
import math
from insec import *

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

    def givesides(self):
        x1, y1, x2, y2 = self.canvas.bbox(self.id)
        return [((x1, y1),(x2, y1)),((x1,y1),(x1,y2)),((x2,y1),(x2,y2)),((x1,y2),(x2,y2))]


class App(object):
    def __init__(self,master):
        self.master = master
        self.canvas = tk.Canvas(root, width = WIDTH, height = HEIGHT)
        self.canvas.pack()

        self.me = self.canvas.create_rectangle(550,400,650,600, fill="blue")


        self.base = (600, 600)

        self.distance = []

        #ADD LIDAR LINES HERE, left to right
        # define line segments, from base to whereever
        self.lasers = [(self.base, (0, 600)),
                       (self.base, (0,800)),
                       (self.base, (0,1000)),
                       (self.base, (300, 1000)),
                       
                      (self.base, (600, 1000)),

                       (self.base, (900, 1000)),
                       (self.base, (1200,1000)),
                       (self.base, (1200, 800)),
                      (self.base, (1200,600))]

        
        for dims in self.lasers:
            self.canvas.create_line(dims[0][0], dims[0][1], dims[1][0], dims[1][1])
            self.distance.append(10000)
        
        self.things = [thingy1(self.canvas, 2,2,300,600)]
        
        self.canvas.pack()
        self.master.after(0,self.animation)

    #determines distance from object if there is one in field of view
    def scan(self):
        disArray = []
        for laser in self.lasers:
            mindist = 10000
            for obj in self.things: #for each object
                sides = obj.givesides()
                for side in sides: #for each side of the object
                    ic = intersects(side, laser)
                    if (ic == (-1,-1)):
                        tempd = 10000
                    else:
                        tempd = math.sqrt((self.base[0] - ic[0])**2 + (self.base[1]-ic[1])**2)
                    #print(tempd)
                    if tempd < mindist:
                        mindist = tempd
            disArray.append(mindist)
        return disArray

    def compareDistance(self, curframe):
        for x in range(len(curframe)):
            #CHANGE THIS AFTER MORE LASERS
            if (curframe[x] < self.distance[x] and self.distance[x]-curframe[x] < 1000):
                self.canvas.itemconfig(self.me, fill='red')
                

    #moves the GUI by one frame and makes calculations, calls itself when it's done
    def animation(self):
        self.canvas.itemconfig(self.me, fill = 'blue')
        for thing in self.things:
            thing.move()
        currentframe = self.scan()
        self.compareDistance(currentframe)
        self.distance = currentframe
        self.master.after(10,self.animation)

    

    
if __name__ == "__main__":
    root = tk.Tk()
    canvas = App(root)
    root.mainloop()
